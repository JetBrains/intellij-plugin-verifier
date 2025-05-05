/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.ide.jps.CompiledIdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdeTheme
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.PluginXmlBuilder
import com.jetbrains.plugin.structure.mocks.modify
import com.jetbrains.plugin.structure.mocks.perfectXmlBuilder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CompiledIdeTest(private val ideManagerType: IdeManagerType) {
  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "ide-manager={0}")
    fun ideManagerType(): List<Array<IdeManagerType>> = listOf(arrayOf(IdeManagerType.DEFAULT), arrayOf(IdeManagerType.JPS))

  }

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideManager: IdeManager

  @Before
  fun setUp() {
    ideManager = when (ideManagerType) {
      IdeManagerType.DEFAULT -> IdeManager.createManager()
      IdeManagerType.JPS -> CompiledIdeManager()
      IdeManagerType.SERVICE_LOADED_JPS -> {
        IdeManagers.loadCompiledIdeManager()
          ?: throw IllegalStateException("Failed to load JPS IDE manager via ServiceLoader")
      }
    }
  }

  /**
   * .m2 (local Maven repository)
   *   com
   *     some
   *       lib-plugin
   *         1.0
   *           lib-plugin-1.0.jar
   *             META-INF
   *               lib-plugin.xml
   *
   * idea/
   *   build.txt (IU-163.1.2.3)
   *   .idea/
   *   community/.idea
   *   out/
   *     classes/
   *       production/
   *         somePlugin/
   *           META-INF/
   *             plugin.xml
   *                references someTheme.theme.json
   *                x-includes "lib-plugin.xml", which resides in Maven repository dependency (com.some:lib-plugin:1.0)
   *         themeHolder
   *           someTheme.theme.json
   */
  @Test
  fun `create idea from ultimate compiled sources`() {
    val m2Directory = buildDirectory(temporaryFolder.newFolder(".m2").toPath()) {
      dir("com") {
        dir("some") {
          dir("lib-plugin") {
            dir("1.0") {
              zip("lib-plugin-1.0.jar") {
                dir("META-INF") {
                  file("lib-plugin.xml") {
                    """
                      <idea-plugin>
                         <extensions defaultExtensionNs="com.intellij">
                            <someExt someKey="someValue"/>
                          </extensions>
                      </idea-plugin>
                    """.trimIndent()
                  }
                }
              }
            }
          }
        }
      }
    }
    val mavenRepository = m2Directory.toAbsolutePath()
    System.setProperty("MAVEN_REPOSITORY", mavenRepository.toString())

    val ideaFolder = buildDirectory(temporaryFolder.newFolder("idea").toPath()) {
      file("build.txt", "IU-163.1.2.3")
      dir(".idea") {
        file("modules.xml") {
          """
            <project version="4">
              <component name="ProjectModuleManager">
                <modules>
                  <module fileurl="file://${'$'}PROJECT_DIR${'$'}/somePlugin/somePlugin.iml" filepath="${'$'}PROJECT_DIR${'$'}/somePlugin/somePlugin.iml"/>
                </modules>
              </component>
            </project>
          """.trimIndent()
        }
      }

      dir("community") {
        dir(".idea") { }
      }

      dir("somePlugin") {
        file("somePlugin.iml") {
          """
            <module type="JAVA_MODULE" version="4">
              <component name="NewModuleRootManager" inherit-compiler-output="true">
                <exclude-output />
                <content url="file://${'$'}PROJECT_DIR${'$'}/somePlugin">
                  <sourceFolder url="file://${'$'}PROJECT_DIR${'$'}/somePlugin/src" isTestSource="false" />
                </content>
                <orderEntry type="inheritedJdk" />
                <orderEntry type="sourceFolder" forTests="false" />
                
                <orderEntry type="module-library" scope="RUNTIME">
                  <library name="lib-plugin" type="repository">
                    <properties maven-id="com.some:lib-plugin:1.0" />
                    <CLASSES>
                      <root url="jar://$mavenRepository/com/some/lib-plugin/1.0/lib-plugin-1.0.jar!/" />
                    </CLASSES>
                  </library>
                </orderEntry>
              </component>
            </module>
          """.trimIndent()
        }
      }

      dir("out") {
        dir("classes") {
          dir("production") {
            dir("somePlugin") {
              dir("META-INF") {
                file("plugin.xml") {
                  perfectXmlBuilder
                    .modify {
                      ideaPluginTagOpen = """<idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">"""

                      additionalContent = """
                          <extensions defaultExtensionNs="com.intellij">
                            <themeProvider id="someId" path="/someTheme.theme.json"/>
                          </extensions>
                          
                          <xi:include href="/META-INF/lib-plugin.xml" xpointer="xpointer(/idea-plugin/*)"/>
                        """.trimIndent()
                    }
                }
              }
            }

            dir("themeHolder") {
              file("someTheme.theme.json") {
                """
                {
                  "name": "someTheme",
                  "dark": true
                }
                """.trimIndent()
              }
            }
          }
        }
      }
    }

    val ide = ideManager.createIde(ideaFolder)
    assertEquals(IdeVersion.createIdeVersion("IU-163.1.2.3"), ide.version)
    assertEquals(1, ide.bundledPlugins.size)

    val plugin = ide.bundledPlugins[0]!!
    assertEquals(ideaFolder.resolve("out").resolve("classes").resolve("production").resolve("somePlugin"), plugin.originalFile)
    assertEquals(listOf(IdeTheme("someTheme", true)), plugin.declaredThemes)
    assertEquals("someId", plugin.pluginId)
  }

  @Test
  fun `plugins bundled to idea may not have versions in descriptors`() {
    val ideaFolder = buildDirectory(temporaryFolder.newFolder("idea").toPath()) {
      file("build.txt", "IU-163.1.2.3")
      dir(".idea") { }
      dir("out") {
        dir("compilation") {
          dir("classes") {
            dir("production") {
              dir("Bundled") {
                dir("META-INF") {
                  file("plugin.xml") {
                    PluginXmlBuilder().modify {
                      name = "<name>Bundled</name>"
                      id = "<id>Bundled</id>"
                      vendor = "<vendor>JetBrains</vendor>"
                      description = "<description>Long enough test description for bundled plugin without version</description>"
                      changeNotes = "<change-notes>Short</change-notes>"
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    val ide = ideManager.createIde(ideaFolder)
    assertEquals(IdeVersion.createIdeVersion("IU-163.1.2.3"), ide.version)
    assertEquals(1, ide.bundledPlugins.size)
    val plugin = ide.bundledPlugins[0]!!
    assertEquals("Bundled", plugin.pluginId)
  }
}