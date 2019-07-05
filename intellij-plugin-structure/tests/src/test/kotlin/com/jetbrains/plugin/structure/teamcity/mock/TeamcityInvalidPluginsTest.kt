package com.jetbrains.plugin.structure.teamcity.mock

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip
import com.jetbrains.plugin.structure.base.problems.UnexpectedDescriptorElements
import com.jetbrains.plugin.structure.teamcity.TeamcityPlugin
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager
import com.jetbrains.plugin.structure.teamcity.problems.ForbiddenWordInPluginName
import com.jetbrains.plugin.structure.teamcity.problems.createIncorrectTeamCityPluginFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import java.io.File

class TeamcityInvalidPluginsTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Rule
  @JvmField
  val expectedEx: ExpectedException = ExpectedException.none()

  @Test
  fun `incorrect plugin file type`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertExpectedProblems(incorrect, listOf(createIncorrectTeamCityPluginFile(incorrect.name)))
  }

  @Test()
  fun `plugin file does not exist`() {
    val nonExistentFile = File("non-existent-file")
    expectedEx.expect(IllegalArgumentException::class.java)
    expectedEx.expectMessage("Plugin file non-existent-file does not exist")
    TeamcityPluginManager.createManager().createPlugin(nonExistentFile)
  }

  @Test
  fun `unable to extract plugin`() {
    val brokenZipArchive = temporaryFolder.newFile("broken.zip")
    assertExpectedProblems(brokenZipArchive, listOf(UnableToExtractZip()))
  }

  @Test
  fun `no meta-inf plugin xml found`() {
    val folder = temporaryFolder.newFolder()
    assertExpectedProblems(folder, listOf(PluginDescriptorIsNotFound("teamcity-plugin.xml")))
  }

  private fun assertExpectedProblems(pluginFile: File, expectedProblems: List<PluginProblem>) {
    val creationFail = getFailedResult(pluginFile)
    assertEquals(expectedProblems, creationFail.errorsAndWarnings)
  }

  private fun getSuccessResult(pluginFile: File): PluginCreationSuccess<TeamcityPlugin> {
    val pluginCreationResult = TeamcityPluginManager.createManager().createPlugin(pluginFile)
    assertTrue(pluginCreationResult is PluginCreationSuccess)
    return pluginCreationResult as PluginCreationSuccess
  }

  private fun getFailedResult(pluginFile: File): PluginCreationFail<TeamcityPlugin> {
    val pluginCreationResult = TeamcityPluginManager.createManager().createPlugin(pluginFile)
    assertTrue(pluginCreationResult is PluginCreationFail)
    return pluginCreationResult as PluginCreationFail
  }


  private fun `test plugin xml warnings`(pluginXmlContent: String, expectedWarnings: List<PluginProblem>) {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    val successResult = getSuccessResult(pluginFolder)
    assertEquals(expectedWarnings, successResult.warnings)
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
        listOf(PropertyNotSpecified("name"))
    )
  }


  @Test
  fun `plugin display name is not specified`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          displayName = ""
        },
        listOf(PropertyNotSpecified("display-name"))
    )
  }

  @Test
  fun `plugin display name contains plugin word`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          displayName = "<display-name>My plugin</display-name>"
        },
        listOf(ForbiddenWordInPluginName)
    )
  }

  @Test
  fun `plugin display name contains teamcity word`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          displayName = "<display-name>Teamcity runner</display-name>"
        },
        listOf(ForbiddenWordInPluginName)
    )
  }

  @Test
  fun `plugin version is not specified`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          version = ""
        },
        listOf(PropertyNotSpecified("version"))
    )
  }
}
