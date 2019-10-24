package com.jetbrains.plugin.structure.domain

import com.jetbrains.plugin.structure.base.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import com.jetbrains.plugin.structure.intellij.plugin.IdeTheme
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.PluginXmlBuilder
import com.jetbrains.plugin.structure.mocks.modify
import com.jetbrains.plugin.structure.mocks.perfectXmlBuilder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import java.io.File

class IdeTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Rule
  @JvmField
  val expectedEx: ExpectedException = ExpectedException.none()

  @Test
  fun `version is not specified in distributed IDE`() {
    val idePath = buildDirectory(temporaryFolder.newFolder("idea")) {
      dir("lib") { }
    }

    val separator = File.separator
    expectedEx.expect(InvalidIdeException::class.java)
    expectedEx.expectMessage(
      "IDE by path '$idePath' is invalid: Build number is not found in the following files relative to $idePath: " +
        "'build.txt', 'Resources${separator}build.txt', 'community${separator}build.txt', 'ultimate${separator}community${separator}build.txt'"
    )

    IdeManager.createManager().createIde(idePath)
  }

  /**
   * idea/
   *  build.txt
   *  plugins/
   *    somePlugin/
   *      META-INF/
   *        plugin.xml
   *  lib/
   *    resources.jar!/
   *      META-INF/
   *        plugin.xml
   */
  @Test
  fun `create idea from binaries`() {
    val ideaFolder = buildDirectory(temporaryFolder.newFolder("idea")) {
      file("build.txt", "IU-163.1.2.3")
      dir("plugins") {
        dir("somePlugin") {
          dir("META-INF") {
            file("plugin.xml") {
              perfectXmlBuilder.modify { }
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
    assertEquals(ideaCorePlugin, ide.getPluginByModule("some.idea.module"))
  }

  /**
   * idea/
   *   build.txt (IU-163.1.2.3)
   *   .idea/
   *   community/.idea
   *   out/
   *     classes/
   *       production/
   *         somePlugin/
   *           META-INF/
   *             plugin.xml (refers someTheme.theme.json)
   *         themeHolder
   *           someTheme.theme.json
   */
  @Test
  fun `create idea from ultimate compiled sources`() {
    val ideaFolder = buildDirectory(temporaryFolder.newFolder("idea")) {
      file("build.txt", "IU-163.1.2.3")
      dir(".idea") { }
      dir("community") {
        dir(".idea") { }
      }

      dir("out") {
        dir("classes") {
          dir("production") {
            dir("somePlugin") {
              dir("META-INF") {
                file("plugin.xml") {
                  perfectXmlBuilder
                    .modify {
                      additionalContent = """
                          <extensions defaultExtensionNs="com.intellij">
                            <themeProvider id="someId" path="/someTheme.theme.json"/>
                          </extensions>
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
    val ideaFolder = buildDirectory(temporaryFolder.newFolder("idea")) {
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
                      description = "<description>Short</description>"
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
}