package com.jetbrains.plugin.structure.domain

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
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
}