package com.jetbrains.plugin.structure.teamcity.mock

import com.jetbrains.plugin.structure.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.plugin.PluginProblem
import com.jetbrains.plugin.structure.problems.*
import com.jetbrains.plugin.structure.teamcity.TeamcityPlugin
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.IllegalArgumentException

class TeamcityInvalidPluginsTest {
  val DESCRIPTOR_PATH = "teamcity-plugin.xml"

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

  @Test()
  fun `plugin file does not exist`() {
    val nonExistentFile = File("non-existent-file")
    expectedEx.expect(IllegalArgumentException::class.java)
    expectedEx.expectMessage("Plugin file non-existent-file does not exist")
    TeamcityPluginManager.createTeamcityPlugin(nonExistentFile)
  }

  @Test
  fun `unable to extract plugin`() {
    val brokenZipArchive = temporaryFolder.newFile("broken.zip")
    assertExpectedProblems(brokenZipArchive, listOf(UnableToExtractZip(brokenZipArchive)))
  }

  @Test
  fun `no meta-inf plugin xml found`() {
    val folder = temporaryFolder.newFolder()
    assertExpectedProblems(folder, listOf(PluginDescriptorIsNotFound(DESCRIPTOR_PATH)))
  }

  private fun assertExpectedProblems(pluginFile: File, expectedProblems: List<PluginProblem>) {
    val creationFail = getFailedResult(pluginFile)
    assertThat(creationFail.errorsAndWarnings, `is`(expectedProblems))
  }

  private fun getSuccessResult(pluginFile: File): PluginCreationSuccess<TeamcityPlugin> {
    val pluginCreationResult = TeamcityPluginManager.createTeamcityPlugin(pluginFile)
    assertThat(pluginCreationResult, instanceOf(PluginCreationSuccess::class.java))
    return pluginCreationResult as PluginCreationSuccess
  }

  private fun getFailedResult(pluginFile: File): PluginCreationFail<TeamcityPlugin> {
    val pluginCreationResult = TeamcityPluginManager.createTeamcityPlugin(pluginFile)
    assertThat(pluginCreationResult, instanceOf(PluginCreationFail::class.java))
    return pluginCreationResult as PluginCreationFail
  }


  private fun `test plugin xml warnings`(pluginXmlContent: String, expectedWarnings: List<PluginProblem>) {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    val successResult = getSuccessResult(pluginFolder)
    assertThat(successResult.warnings, `is`(expectedWarnings))
  }

  private fun `test invalid plugin xml`(pluginXmlContent: String, expectedProblems: List<PluginProblem>) {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    assertExpectedProblems(pluginFolder, expectedProblems)
  }

  private fun `test valid plugin xml`(pluginXmlContent: String) {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    getSuccessResult(pluginFolder)
  }

  private fun getTempPluginFolder(pluginXmlContent: String): File {
    val pluginFolder = temporaryFolder.newFolder()
    pluginFolder.mkdirs()
    File(pluginFolder, "teamcity-plugin.xml").writeText(pluginXmlContent)
    return pluginFolder
  }

  @Test
  fun `completely invalid plugin descriptor`() {
    `test invalid plugin xml`(
        "abracadabra",
        listOf(UnexpectedDescriptorElements("unexpected element on line 1"))
    )
  }

  @Test
  fun `plugin name is not specified`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          name = ""
        },
        listOf(PropertyNotSpecified("name")))
  }

  @Test
  fun `plugin display name is not specified`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          displayName = ""
        },
        listOf(PropertyNotSpecified("display-name")))
  }

  @Test
  fun `plugin version is not specified`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          version = ""
        },
        listOf(PropertyNotSpecified("version")))
  }
}
