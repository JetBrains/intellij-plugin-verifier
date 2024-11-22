package com.jetbrains.plugin.structure.domain

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import com.jetbrains.plugin.structure.intellij.plugin.IdeTheme
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.PluginXmlBuilder
import com.jetbrains.plugin.structure.mocks.modify
import com.jetbrains.plugin.structure.mocks.perfectXmlBuilder
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Paths

class IdeTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `version is not specified in distributed IDE`() {
    val idePath = buildDirectory(temporaryFolder.newFolder("idea").toPath()) {
      dir("lib") { }
    }

    Assert.assertThrows(
      "IDE by path '$idePath' is invalid: Build number is not found in the following files relative to $idePath: " +
        "'build.txt', '${Paths.get("Resources").resolve("build.txt")}', '${Paths.get("community").resolve("build.txt")}', '${Paths.get("ultimate").resolve("community").resolve("build.txt")}'",
      InvalidIdeException::class.java
    ) {
      IdeManager.createManager().createIde(idePath)
    }
  }

  /**
   * idea/
   *  build.txt
   *  plugins/
   *    somePlugin/
   *      lib/
   *        somePlugin.jar
   *          META-INF/
   *            plugin.xml
   *              -- references 'inRootXml.xml'
   *          inRootXml.xml
   *  lib/
   *    resources.jar!/
   *      META-INF/
   *        plugin.xml
   */
  @Test
  fun `create idea from binaries`() {
    val ideaFolder = buildDirectory(temporaryFolder.newFolder("idea").toPath()) {
      file("build.txt", "IU-163.1.2.3")
      dir("plugins") {
        dir("somePlugin") {
          dir("lib") {
            zip("somePlugin.jar") {
              dir("META-INF") {
                file("plugin.xml") {
                  perfectXmlBuilder.modify {
                    ideaPluginTagOpen = """<idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">"""

                    additionalContent = """
                      <xi:include href="inRootXml.xml" xpointer="xpointer(/idea-plugin/*)"/>
                    """.trimIndent()
                  }
                }
              }
              file("inRootXml.xml") {
                """
                  <idea-plugin>
                     <extensions defaultExtensionNs="com.intellij">
                        <inRootExt someKey="inRootKey"/>
                      </extensions>
                  </idea-plugin>
                """.trimIndent()
              }
            }
          }
        }
      }

      dir("lib") {
        zip("resources.jar") {
          dir("META-INF") {
            file("plugin.xml") {
              perfectXmlBuilder.modify {
                id = "<id>com.intellij</id>"
                name = "<name>IDEA CORE</name>"
                modules = listOf("some.idea.module")
              }
            }
          }
        }
      }
    }

    val ide = IdeManager.createManager().createIde(ideaFolder)
    assertEquals(IdeVersion.createIdeVersion("IU-163.1.2.3"), ide.version)
    assertEquals(2, ide.bundledPlugins.size)
    val bundledPlugin = ide.bundledPlugins[0]!!
    val ideaCorePlugin = ide.bundledPlugins[1]!!
    assertEquals("someId", bundledPlugin.pluginId)
    assertEquals("com.intellij", ideaCorePlugin.pluginId)
    assertEquals("IDEA CORE", ideaCorePlugin.pluginName)
    assertEquals("some.idea.module", ideaCorePlugin.definedModules.single())
    assertEquals(ideaCorePlugin, ide.findPluginByModule("some.idea.module"))
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

    val ide = IdeManager.createManager().createIde(ideaFolder)
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

    val ide = IdeManager.createManager().createIde(ideaFolder)
    assertEquals(IdeVersion.createIdeVersion("IU-163.1.2.3"), ide.version)
    assertEquals(1, ide.bundledPlugins.size)
    val plugin = ide.bundledPlugins[0]!!
    assertEquals("Bundled", plugin.pluginId)
  }

  /**
   * idea-IE/
   *  build.txt
   *  plugins/
   *    somePlugin/
   *      lib/
   *        somePlugin.jar
   *          META-INF/
   *            plugin.xml
   *              -- references 'inRootXml.xml'
   *          inRootXml.xml
   *  lib/
   *    app.jar!/
   *      META-INF/
   *        PlatformLangPlugin.xml
   */
  @Test
  fun `create idea IE from binaries`() {
    val ideaFolder = buildDirectory(temporaryFolder.newFolder("idea-IE").toPath()) {
      file("build.txt", "IE-221.5591.62")

      dir("lib") {
        zip("app.jar") {
          dir("META-INF") {
            file("PlatformLangPlugin.xml") {
              perfectXmlBuilder.modify {
                id = "<id>com.intellij</id>"
                name = "<name>IDEA CORE</name>"
                modules = listOf("some.idea.module")
              }
            }
          }
        }
      }
    }

    val ide = IdeManager.createManager().createIde(ideaFolder)
    assertEquals(IdeVersion.createIdeVersion("IE-221.5591.62"), ide.version)
    assertEquals(1, ide.bundledPlugins.size)
    val ideaCorePlugin = ide.bundledPlugins[0]!!
    assertEquals("com.intellij", ideaCorePlugin.pluginId)
    assertEquals("IDEA CORE", ideaCorePlugin.pluginName)
    assertEquals("some.idea.module", ideaCorePlugin.definedModules.single())
    assertEquals(ideaCorePlugin, ide.findPluginByModule("some.idea.module"))
  }
}