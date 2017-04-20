package com.intellij.structure.domain

import com.intellij.structure.ide.IdeManager
import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.mocks.perfectXmlBuilder
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.collection.IsCollectionWithSize.hasSize
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
  fun `version is not specified`() {
    val ideaFolder = temporaryFolder.newFolder()

    expectedEx.expect(IllegalArgumentException::class.java)
    expectedEx.expectMessage("$ideaFolder/build.txt is not found")

    IdeManager.getInstance().createIde(ideaFolder)
  }

  @Test
  fun `create idea from binaries`() {
    val bundledPluginXmlContent = perfectXmlBuilder.asString()

    val ideaFolder = temporaryFolder.newFolder("idea")
    File(ideaFolder, "build.txt").writeText("IU-163.1.2.3")

    val pluginsFolder = File(ideaFolder, "plugins")
    val somePluginFolder = File(pluginsFolder, "somePlugin")
    val bundledXml = File(somePluginFolder, "META-INF/plugin.xml")
    bundledXml.parentFile.mkdirs()
    bundledXml.writeText(bundledPluginXmlContent)

    val ide = IdeManager.getInstance().createIde(ideaFolder)
    assertThat(ide.version, `is`(IdeVersion.createIdeVersion("IU-163.1.2.3")))
    assertThat(ide.bundledPlugins, hasSize(1))
    val plugin = ide.bundledPlugins[0]!!
    assertThat(plugin.pluginId, `is`("someId"))
  }

  @Test
  fun `create idea from ultimate compiled sources`() {
    val bundledPluginXmlContent = perfectXmlBuilder.asString()

    val ideaFolder = temporaryFolder.newFolder("idea")
    File(ideaFolder, "build.txt").writeText("IU-163.1.2.3")

    File(ideaFolder, ".idea").mkdirs()
    File(ideaFolder, "community/.idea").mkdirs()

    val productionDir = File(ideaFolder, "out/classes/production")
    productionDir.mkdirs()

    val bundledFolder = File(productionDir, "somePlugin")
    val bundledXml = File(bundledFolder, "/META-INF/plugin.xml")
    bundledXml.parentFile.mkdirs()
    bundledXml.writeText(bundledPluginXmlContent)

    val ide = IdeManager.getInstance().createIde(ideaFolder)
    assertThat(ide.version, `is`(IdeVersion.createIdeVersion("IU-163.1.2.3")))
    assertThat(ide.bundledPlugins, hasSize(1))

    val plugin = ide.bundledPlugins[0]!!
    assertThat(plugin.pluginId, `is`("someId"))
    assertThat(plugin.originalFile, `is`(bundledFolder))
  }
}