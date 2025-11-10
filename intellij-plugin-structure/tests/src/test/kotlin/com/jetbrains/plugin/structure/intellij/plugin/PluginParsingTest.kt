/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.zip.ZipEntrySpec
import com.jetbrains.plugin.structure.base.zip.ZipEntrySpec.File
import com.jetbrains.plugin.structure.base.zip.ZipEntrySpec.Plain
import com.jetbrains.plugin.structure.base.zip.createZip
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.IdePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Path

class PluginParsingTest(fileSystemType: FileSystemType) : IdePluginManagerTest(fileSystemType) {

  @Test
  fun `IdePlugin is correctly built from file`() {
    val plugin = createPlugin {
      dir("plugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file("plugin.xml") { """
                <idea-plugin>
                  <id>someId</id>
                  <name>someName</name>
                  <version>2.1</version>
                  <vendor>JetBrains</vendor>
                  <idea-version since-build="131.1" until-build="252.2"/>
                  <module value="main.alias"/>
                  <incompatible-with>incompatible.id</incompatible-with>
                  <depends>com.intellij.modules.lang</depends>
                  <depends optional="true" config-file="depends.xml">Git4Idea</depends>
                  <dependencies>
                    <plugin id="from.main"/>
                    <module name="from.main.module"/>
                  </dependencies>
                  <content>
                    <module name="someId.opt"/>
                    <module name="someId.req" loading="required"/>
                  </content>
                </idea-plugin>
              """.trimIndent()
              }
              file("depends.xml") {
                """
                  <idea-plugin>
                    <depends>from.depends</depends>
                  </idea-plugin>
                """.trimIndent()
              }
            }
            file("someId.opt.xml") {
              """
                <idea-plugin>
                  <module value="opt.alias"/>
                  <dependencies>
                    <plugin id="from.opt"/>
                    <module name="from.opt.module"/>
                    <module name="someId.req"/>
                  </dependencies>
                </idea-plugin>
              """.trimIndent()
            }
            file("someId.req.xml") { """
              <idea-plugin>
                <incompatible-with>incompatible.id.req</incompatible-with>
                <dependencies>
                  <plugin id="from.req"/>
                  <module name="from.req.module"/>
                </dependencies>
              </idea-plugin>
            """.trimIndent() }
          }
        }
      }
    }
    assertEquals("someId", plugin.pluginId)
    assertEquals("someName", plugin.pluginName)
    assertEquals("2.1", plugin.pluginVersion)
    assertEquals("JetBrains", plugin.vendor)
    assertEquals(IdeVersion.createIdeVersion("131.1"), plugin.sinceBuild)
    assertEquals(IdeVersion.createIdeVersion("252.2"), plugin.untilBuild)
    assertEquals(1, plugin.pluginAliases.size)
    assertEquals("main.alias", plugin.pluginAliases.first())
    assertEquals(1, plugin.incompatibleWith.size)
    assertEquals("incompatible.id", plugin.incompatibleWith.first())

    assertEquals(2, plugin.dependsList.size)
    plugin.dependsList[0].let { dependsStatement ->
      assertEquals("com.intellij.modules.lang", dependsStatement.pluginId)
      assertEquals(false, dependsStatement.isOptional)
      assertEquals(null, dependsStatement.configFile)
    }
    plugin.dependsList[1].let { dependsStatement ->
      assertEquals("Git4Idea", dependsStatement.pluginId)
      assertEquals(true, dependsStatement.isOptional)
      assertEquals("depends.xml", dependsStatement.configFile)
    }

    assertEquals(1, plugin.pluginMainModuleDependencies.size)
    assertEquals("from.main", plugin.pluginMainModuleDependencies[0].pluginId)

    assertEquals(1, plugin.contentModuleDependencies.size)
    assertEquals("from.main.module", plugin.contentModuleDependencies[0].moduleName)

    val dependsSubDescriptor = plugin.optionalDescriptors.single().optionalPlugin
    assertEquals(1, dependsSubDescriptor.dependsList.size)
    assertEquals("from.depends", dependsSubDescriptor.dependsList[0].pluginId)

    assertEquals(2, plugin.modulesDescriptors.size)
    plugin.modulesDescriptors[0].let { contentModule ->
      val module = contentModule.module
      assertEquals("someId.opt", contentModule.name)
      assertEquals(1, module.pluginMainModuleDependencies.size)
      assertEquals("from.opt", module.pluginMainModuleDependencies[0].pluginId)
      assertEquals(2, module.contentModuleDependencies.size)
      assertEquals("from.opt.module", module.contentModuleDependencies[0].moduleName)
      assertEquals("someId.req", module.contentModuleDependencies[1].moduleName)

      assertEquals(1, module.pluginAliases.size)
      assertEquals("opt.alias", module.pluginAliases.first())
    }
    plugin.modulesDescriptors[1].let { contentModule ->
      val module = contentModule.module
      assertEquals("someId.req", contentModule.name)
      assertEquals(1, module.pluginMainModuleDependencies.size)
      assertEquals("from.req", module.pluginMainModuleDependencies[0].pluginId)
      assertEquals(1, module.contentModuleDependencies.size)
      assertEquals("from.req.module", module.contentModuleDependencies[0].moduleName)

      assertEquals(1, module.incompatibleWith.size)
      assertEquals("incompatible.id.req", module.incompatibleWith.first())
    }
  }

  @Test
  fun `namespaces and visibility of content modules is handled properly`() {
    val plugin = createPlugin {
      dir("plugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file("plugin.xml") { """
                <idea-plugin>
                  <id>someId</id>
                  <content namespace="my_namespace">
                    <module name="module.private"/>
                    <module name="module.internal"/>
                    <module name="module.public"/>
                    <module name="module.default"/>
                  </content>
                </idea-plugin>  
              """.trimIndent()
              }
            }
            file("module.private.xml") { """
                <idea-plugin visibility="private">
                  <dependencies>
                    <module name="module.internal"/>
                    <module name="another" namespace="another_namespace"/>
                    <module name="platform.module"/>
                  </dependencies>
                </idea-plugin>
              """.trimIndent()
            }
            file("module.internal.xml") {
              """<idea-plugin visibility="internal"/>"""
            }
            file("module.public.xml") {
              """<idea-plugin visibility="public"/>"""
            }
            file("module.default.xml") {
              """<idea-plugin/>"""
            }
          }
        }
      }
    }
    assertEquals("my_namespace", plugin.contentModules[0].namespace)
    assertEquals("my_namespace", plugin.contentModules[0].actualNamespace)
    assertEquals(4, plugin.modulesDescriptors.size)
    val modulePrivate = plugin.modulesDescriptors[0]
    assertEquals("module.private", modulePrivate.name)
    assertEquals(ModuleVisibility.PRIVATE, modulePrivate.module.moduleVisibility)
    assertEquals(3, modulePrivate.module.contentModuleDependencies.size)
    val dependencies = modulePrivate.module.contentModuleDependencies
    assertEquals("module.internal", dependencies[0].moduleName)
    assertEquals("my_namespace", dependencies[0].namespace)
    assertEquals("another", dependencies[1].moduleName)
    assertEquals("another_namespace", dependencies[1].namespace)
    assertEquals("platform.module", dependencies[2].moduleName)
    assertEquals("jetbrains", dependencies[2].namespace)
    val moduleInternal = plugin.modulesDescriptors[1]
    assertEquals("module.internal", moduleInternal.name)
    assertEquals(ModuleVisibility.INTERNAL, moduleInternal.module.moduleVisibility)
    val modulePublic = plugin.modulesDescriptors[2]
    assertEquals("module.public", modulePublic.name)
    assertEquals(ModuleVisibility.PUBLIC, modulePublic.module.moduleVisibility)
    val moduleDefault = plugin.modulesDescriptors[3]
    assertEquals("module.default", moduleDefault.name)
    assertEquals(ModuleVisibility.PRIVATE, moduleDefault.module.moduleVisibility)
  }

  @Test
  fun `implicit namespace for private modules`() {
    val plugin = createPlugin {
      dir("plugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file("plugin.xml") { """
                <idea-plugin>
                  <id>someId</id>
                  <content>
                    <module name="module1"/>
                    <module name="module2"/>
                  </content>
                </idea-plugin>  
              """.trimIndent()
              }
            }
            file("module1.xml") { """
                <idea-plugin>
                  <dependencies>
                    <module name="module2"/>
                  </dependencies>
                </idea-plugin>
              """.trimIndent()
            }
            file("module2.xml") {
              """<idea-plugin/>"""
            }
          }
        }
      }
    }
    assertNull(plugin.contentModules[0].namespace)
    assertEquals("someId_\$implicit", plugin.contentModules[0].actualNamespace)
    assertEquals(2, plugin.modulesDescriptors.size)
    val module1 = plugin.modulesDescriptors[0]
    assertEquals("module1", module1.name)
    val dependency = module1.module.contentModuleDependencies.single()
    assertEquals("module2", dependency.moduleName)
    assertEquals("someId_\$implicit", dependency.namespace)
  }

  @Test
  fun `plugin descriptor contains BOM`() {
    val pluginXml = "<idea-plugin />"
    val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    val pluginXmlBytes = bom + pluginXml.toByteArray(Charsets.UTF_8)

    createPlugin {
      dir("plugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file("plugin.xml", pluginXmlBytes)
            }
          }
        }
      }
    }
  }

  @Test
  fun `plugin descriptor contains BOM in an dependent descriptr`() {
    val secondaryDescriptor = "<idea-plugin />"
    val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    val secondaryDescriptorBytes = bom + secondaryDescriptor.toByteArray(Charsets.UTF_8)

    val pluginXml = """
      <idea-plugin>
          <depends optional="true" config-file="git4idea-integration.xml">Git4Idea</depends>      
      </idea-plugin>          
    """.trimIndent()

    val plugin = createPlugin {
      dir("plugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file("plugin.xml", pluginXml)
              file("git4idea-integration.xml", secondaryDescriptorBytes)
            }
          }
        }
      }
    }
    with(plugin.optionalDescriptors) {
      assertEquals(1, size)
      val git4IdeaDescriptor = first()
      assertEquals("Git4Idea", git4IdeaDescriptor.dependency.id)
    }
  }

  @Test
  fun `ZIP contains a file starting two dots, but this is allowed, unless there is a directory separator`() {
    createPlugin {
      file("..md")
      dir("plugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file("plugin.xml", "<idea-plugin />")
            }
          }
        }
      }
    }
  }

  @Test
  fun `ZIP file contains a directory traversal via two dots`() {
    val pluginJarPath = temporaryFolder.newFile("plugin.jar")
    createZip(pluginJarPath, Plain("META-INF/plugin.xml", "<idea-plugin />"))

    val pluginFactory = { pluginManager: IdePluginManager, pluginArtifactPath: Path ->
      pluginManager.createPlugin(
        pluginArtifactPath,
        validateDescriptor = true,
        problemResolver = AnyProblemToWarningPluginCreationResultResolver,
      )
    }
    val pluginManager = createManager(extractedDirectory)
    val contentBase = listOf(File("plugin/lib/plugin.jar", pluginJarPath))
    val wrongScenarios = listOf(
      Plain("..", "") to "The plugin archive file cannot be extracted. " +
        "Invalid relative entry name: path traversal outside root of archive in [..]",
      Plain("META-INF/../..", "") to "The plugin archive file cannot be extracted. " +
        "Invalid relative entry name: path traversal outside root of archive in [META-INF/../..]",
      Plain(".", "") to "The plugin archive file cannot be extracted. " +
        "Resolved entry name cannot be empty: [.]",
      Plain("././.", "") to "The plugin archive file cannot be extracted. " +
        "Resolved entry name cannot be empty: [././.]",
      Plain(".hidden/.", "") to "The plugin root directory must not contain multiple files: .hidden, plugin. " +
        "The plugin .jar file should be placed in the 'lib' folder within the plugin's root directory, along with all the required bundled libraries.",
    )

    wrongScenarios.forEach {(spec, expectedMessage) ->
      val randomNumber = System.currentTimeMillis()
      val zipPath = temporaryFolder.newFile("plugin$randomNumber.zip")
      createZip(zipPath, *(contentBase + spec).toTypedArray())
      val pluginCreationResult = createPlugin(pluginManager, zipPath, pluginFactory)
      if (pluginCreationResult is PluginCreationFail) {
        assertEquals(expectedMessage, pluginCreationResult.errorsAndWarnings.firstOrNull()?.message)
      }
    }
  }

  private fun createPlugin(content: ContentBuilder.() -> Unit): IdePlugin {
    val pluginFactory = { pluginManager: IdePluginManager, pluginArtifactPath: Path ->
      pluginManager.createPlugin(
        pluginArtifactPath,
        validateDescriptor = true,
        problemResolver = AnyProblemToWarningPluginCreationResultResolver,
      )
    }
    val pluginArtifactPath = buildZipFile(temporaryFolder.newFile("plugin.zip"), content)
    val successResult = createPluginSuccessfully(pluginArtifactPath, pluginFactory)
    return successResult.plugin
  }

  @After
  fun tearDown() {
    close()
  }
}