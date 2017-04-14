package com.intellij.structure.mocks

import com.intellij.structure.plugin.PluginCreationFail
import com.intellij.structure.plugin.PluginManager
import com.intellij.structure.problems.*
import org.codehaus.plexus.archiver.jar.JarArchiver
import org.codehaus.plexus.logging.Logger
import org.codehaus.plexus.logging.console.ConsoleLogger
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.IllegalArgumentException

class InvalidPluginsTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Rule
  @JvmField
  val expectedEx: ExpectedException = ExpectedException.none()

  @Test
  fun `incorrect plugin file type`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertExpectedProblems(incorrect, listOf(IncorrectPluginFile(incorrect)))
  }

  @Test
  fun `failed to read jar file`() {
    val incorrect = temporaryFolder.newFile("incorrect.jar")
    assertExpectedProblems(incorrect, listOf(UnableToReadJarFile(incorrect)))
  }

  @Test()
  fun `plugin file does not exist`() {
    val nonExistentFile = File("non-existent-file")
    expectedEx.expect(IllegalArgumentException::class.java)
    expectedEx.expectMessage("Plugin file non-existent-file does not exist")
    PluginManager.getInstance().createPlugin(nonExistentFile)
  }

  @Test
  fun `unable to extract plugin`() {
    val brokenZipArchive = temporaryFolder.newFile("broken.zip")
    assertExpectedProblems(brokenZipArchive, listOf(UnableToExtractZip(brokenZipArchive)))
  }

  @Test
  fun `no meta-inf plugin xml found`() {
    val folder = temporaryFolder.newFolder()
    assertExpectedProblems(folder, listOf(PluginDescriptorIsNotFound("plugin.xml")))
  }

  private fun assertExpectedProblems(pluginFile: File, expectedProblems: List<PluginProblem>) {
    val creationFail = getFailedResult(pluginFile)
    assertThat(creationFail.errorsAndWarnings, `is`(expectedProblems))
  }

  private fun getFailedResult(pluginFile: File): PluginCreationFail {
    val pluginCreationResult = PluginManager.getInstance().createPlugin(pluginFile)
    assertThat(pluginCreationResult, instanceOf(PluginCreationFail::class.java))
    return pluginCreationResult as PluginCreationFail
  }

  @Test
  fun `plugin description is empty`() {
    `test invalid plugin xml`("""<idea-plugin>
          <id>someId</id>
          <name>someName</name>
          <version>someVersion</version>
          <vendor email="vendor.com" url="url">vendor</vendor>
          <description></description>
          <idea-version since-build="131"/>
      </idea-plugin>
      """, listOf(EmptyDescription("plugin.xml")))
  }

  @Test
  fun `plugin name is not specified`() {
    `test invalid plugin xml`("""<idea-plugin>
          <id>someId</id>
          <version>someVersion</version>
          <vendor email="vendor.com" url="url">vendor</vendor>
          <description>d</description>
          <idea-version since-build="131"/>
      </idea-plugin>
      """, listOf(PluginNameIsNotSpecified("plugin.xml")))
  }

  @Test
  fun `plugin vendor is not specified`() {
    `test invalid plugin xml`("""<idea-plugin>
          <id>someId</id>
          <name>someId</name>
          <version>someVersion</version>
          <description>d</description>
          <idea-version since-build="131"/>
      </idea-plugin>
      """, listOf(VendorIsNotSpecified("plugin.xml")))
  }

  @Test
  fun `plugin version is not specified`() {
    `test invalid plugin xml`("""<idea-plugin>
          <id>someId</id>
          <name>someName</name>
          <vendor email="vendor.com" url="url">vendor</vendor>
          <description>d</description>
          <idea-version since-build="131"/>
      </idea-plugin>
      """, listOf(VersionIsNotSpecified("plugin.xml")))
  }

  @Test
  fun `idea version is not specified`() {
    `test invalid plugin xml`("""<idea-plugin>
          <id>someId</id>
          <name>someName</name>
          <version>someVersion</version>
          <vendor email="vendor.com" url="url">vendor</vendor>
          <description>d</description>
      </idea-plugin>
      """, listOf(IdeaVersionIsNotSpecified("plugin.xml")))
  }

  @Test
  fun `invalid dependency bean`() {
    `test invalid plugin xml`("""<idea-plugin>
          <id>someId</id>
          <name>someName</name>
          <version>someVersion</version>
          <vendor email="vendor.com" url="url">vendor</vendor>
          <description>d</description>
          <idea-version since-build="131"/>
          <depends></depends>
      </idea-plugin>
      """, listOf(InvalidDependencyBean("plugin.xml")))
  }

  @Test
  fun `invalid module bean`() {
    `test invalid plugin xml`("""<idea-plugin>
          <id>someId</id>
          <name>someName</name>
          <version>someVersion</version>
          <vendor email="vendor.com" url="url">vendor</vendor>
          <description>d</description>
          <idea-version since-build="131"/>
          <module></module>
      </idea-plugin>
      """, listOf(InvalidModuleBean("plugin.xml")))
  }


  private fun `test invalid plugin xml`(pluginXmlContent: String, expectedProblems: List<PluginProblem>) {
    val pluginFolder = temporaryFolder.newFolder()
    val metaInf = File(pluginFolder, "META-INF")
    metaInf.mkdirs()
    File(metaInf, "plugin.xml").writeText(pluginXmlContent)
    assertExpectedProblems(pluginFolder, expectedProblems)
  }

  @Test
  fun `plugin has multiple plugin descriptors in lib directory`() {
    /*
      plugin/
      plugin/lib
      plugin/lib/one.jar!/META-INF/plugin.xml
      plugin/lib/two.jar!/META-INF/plugin.xml
    */
    val pluginOneContent = """<idea-plugin>
        <id>one</id>
        <name>one</name>
        <version>one</version>
        <vendor email="vendor.com" url="url">one</vendor>
        <description>one</description>
        <idea-version since-build="131"/>
    </idea-plugin>
    """

    val pluginTwoContent = """<idea-plugin>
        <id>two</id>
        <name>two</name>
        <version>two</version>
        <vendor email="vendor.com" url="url">two</vendor>
        <description>two</description>
        <idea-version since-build="131"/>
    </idea-plugin>
    """

    val pluginFolder = temporaryFolder.newFolder()
    val lib = File(pluginFolder, "lib")
    lib.mkdirs()

    val oneMetaInf = temporaryFolder.newFolder("one", "META-INF")
    File(oneMetaInf, "plugin.xml").writeText(pluginOneContent)
    archiveDirectoryInto(oneMetaInf, File(lib, "one.jar"))

    val twoMetaInf = temporaryFolder.newFolder("two", "META-INF")
    File(twoMetaInf, "plugin.xml").writeText(pluginTwoContent)
    archiveDirectoryInto(twoMetaInf, File(lib, "two.jar"))

    assertExpectedProblems(pluginFolder, listOf(MultiplePluginDescriptorsInLibDirectory("one.jar", "two.jar")))
  }

  private fun archiveDirectoryInto(directory: File, destinationJar: File) {
    val jarArchiver = JarArchiver()
    jarArchiver.enableLogging(ConsoleLogger(Logger.LEVEL_ERROR, "Unarchive logger"))
    jarArchiver.addDirectory(directory, directory.name + "/")
    jarArchiver.destFile = destinationJar
    jarArchiver.createArchive()
  }
}
