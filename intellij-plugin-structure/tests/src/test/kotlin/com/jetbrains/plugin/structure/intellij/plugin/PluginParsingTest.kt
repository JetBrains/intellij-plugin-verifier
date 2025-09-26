/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.IdePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path

class PluginParsingTest(fileSystemType: FileSystemType) : IdePluginManagerTest(fileSystemType) {

  @Test
  fun `IdePlugin is correctly built from file`() {
    val pluginFactory = { pluginManager: IdePluginManager, pluginArtifactPath: Path ->
      pluginManager.createPlugin(
        pluginArtifactPath,
        validateDescriptor = true,
        problemResolver = AnyProblemToWarningPluginCreationResultResolver,
      )
    }
    val pluginArtifactPath = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
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
    val successResult = createPluginSuccessfully(pluginArtifactPath, pluginFactory)
    val plugin = successResult.plugin

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

  @After
  fun tearDown() {
    close()
  }
}