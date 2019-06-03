package com.jetbrains.plugin.structure.domain

import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import com.jetbrains.plugin.structure.intellij.plugin.IdeTheme
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.PluginXmlBuilder
import com.jetbrains.plugin.structure.mocks.modify
import com.jetbrains.plugin.structure.mocks.perfectXmlBuilder
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
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
    val idePath = temporaryFolder.newFolder("idea")
    idePath.resolve("lib").mkdirs()

    val separator = File.separator
    expectedEx.expect(InvalidIdeException::class.java)
    expectedEx.expectMessage(
        "IDE by path '$idePath' is invalid: Build number is not found in the following files relative to $idePath: " +
            "'build.txt', 'Resources${separator}build.txt', 'community${separator}build.txt', 'ultimate${separator}community${separator}build.txt'"
    )

    IdeManager.createManager().createIde(idePath)
  }

  @Test
  fun `create idea from binaries`() {
    val ideaFolder = temporaryFolder.newFolder("idea")
    File(ideaFolder, "build.txt").writeText("IU-163.1.2.3")

    /**
     * Create /plugins/somePlugin/META-INF/plugin.xml
     * of some bundled plugin.
     */
    val pluginsFolder = File(ideaFolder, "plugins")
    val somePluginFolder = File(pluginsFolder, "somePlugin")

    val bundledPluginXmlContent = perfectXmlBuilder.asString()
    val bundledXml = File(somePluginFolder, "META-INF/plugin.xml")
    bundledXml.parentFile.mkdirs()
    bundledXml.writeText(bundledPluginXmlContent)

    /**
     * Create a /lib/resources.jar/META-INF/plugin.xml
     * that contains the `IDEA-CORE` plugin.
     */
    val ideaPluginXml = temporaryFolder.newFolder().resolve("META-INF").resolve("plugin.xml")
    ideaPluginXml.parentFile.mkdirs()
    ideaPluginXml.writeText(perfectXmlBuilder.apply {
      id = "<id>com.intellij</id>"
      name = "<name>IDEA CORE</name>"
      modules = listOf("some.idea.module")
    }.asString())

    val resourcesJar = ideaFolder.resolve("lib").resolve("resources.jar")
    resourcesJar.parentFile.mkdirs()
    JarFileUtils.createJarFile(
        listOf(
            JarFileEntry(ideaPluginXml, "META-INF/plugin.xml")
        ),
        resourcesJar
    )

    val ide = IdeManager.createManager().createIde(ideaFolder)
    assertThat(ide.version, `is`(IdeVersion.createIdeVersion("IU-163.1.2.3")))
    assertThat(ide.bundledPlugins, hasSize(2))
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
    val ideaFolder = temporaryFolder.newFolder("idea")
    ideaFolder.resolve("build.txt").writeText("IU-163.1.2.3")

    ideaFolder.resolve(".idea").mkdirs()
    ideaFolder.resolve("community/.idea").mkdirs()

    val modulesRoot = ideaFolder.resolve("out/classes/production")
    modulesRoot.mkdirs()

    val bundledPluginFolder = modulesRoot.resolve("somePlugin")
    val bundledXml = bundledPluginFolder.resolve("META-INF").resolve("plugin.xml")
    bundledXml.parentFile.mkdirs()

    val bundledPluginXmlContent = perfectXmlBuilder
        .modify {
          additionalContent = """
            <extensions defaultExtensionNs="com.intellij">
              <themeProvider id="someId" path="/someTheme.theme.json"/>
            </extensions>
          """.trimIndent()
        }
    bundledXml.writeText(bundledPluginXmlContent)

    val themeJson = modulesRoot.resolve("themeHolder").resolve("someTheme.theme.json")
    themeJson.parentFile.mkdirs()
    themeJson.writeText(
        """{
            "name": "someTheme",
            "dark": true
           }
    """.trimIndent()
    )

    val ide = IdeManager.createManager().createIde(ideaFolder)
    assertThat(ide.version, `is`(IdeVersion.createIdeVersion("IU-163.1.2.3")))
    assertThat(ide.bundledPlugins, hasSize(1))

    val plugin = ide.bundledPlugins[0]!!
    assertThat(plugin.pluginId, `is`("someId"))
    assertThat(plugin.originalFile, `is`(bundledPluginFolder))
    assertThat(plugin.declaredThemes, `is`(listOf(IdeTheme("someTheme", true))))
  }

  @Test
  fun `plugins bundled to idea may not have versions in descriptors`() {
    val ideaFolder = temporaryFolder.newFolder("idea")
    ideaFolder.resolve(".idea").mkdirs()

    val compilationRoot = ideaFolder.resolve("out").resolve("compilation").resolve("classes").resolve("production")
    compilationRoot.mkdirs()
    ideaFolder.resolve("build.txt").writeText("IU-163.1.2.3")

    val incompleteDescriptor = PluginXmlBuilder().modify {
      name = "<name>Bundled</name>"
      id = "<id>Bundled</id>"
      vendor = "<vendor>JetBrains</vendor>"
      description = "<description>Short</description>"
      changeNotes = "<change-notes>Short</change-notes>"
    }
    val bundledPluginFolder = compilationRoot.resolve("Bundled")
    val bundledXml = bundledPluginFolder.resolve("META-INF/plugin.xml")
    bundledXml.parentFile.mkdirs()
    bundledXml.writeText(incompleteDescriptor)

    val ide = IdeManager.createManager().createIde(ideaFolder)
    assertEquals(IdeVersion.createIdeVersion("IU-163.1.2.3"), ide.version)
    assertEquals(1, ide.bundledPlugins.size)
    val plugin = ide.bundledPlugins[0]!!
    assertThat(plugin.pluginId, `is`("Bundled"))
  }
}