package com.jetbrains.plugin.structure.edu.mock

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.plugin.structure.edu.*
import com.jetbrains.plugin.structure.edu.problems.InvalidVersionError
import com.jetbrains.plugin.structure.edu.problems.UnsupportedLanguage
import com.jetbrains.plugin.structure.edu.problems.UnsupportedProgrammingLanguage
import com.jetbrains.plugin.structure.edu.problems.createIncorrectEduPluginFile
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File


class EduInvalidPluginsTest {
  companion object {
    fun assertExpectedProblems(pluginFile: File, expectedProblems: List<PluginProblem>) {
      try {
        val creationFail = getFailedResult(pluginFile)
        val actualProblems = creationFail.errorsAndWarnings
        Assert.assertEquals(expectedProblems.toSet(), actualProblems.toSet())
      } finally {
        pluginFile.deleteLogged()
      }
    }

    private fun getFailedResult(pluginFile: File): PluginCreationFail<EduPlugin> {
      val pluginCreationResult = EduPluginManager.createManager().createPlugin(pluginFile)
      if (pluginCreationResult is PluginCreationSuccess) {
        Assert.fail("must have failed, but warnings: [${pluginCreationResult.warnings.joinToString()}]")
      }
      return pluginCreationResult as PluginCreationFail
    }
  }

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test(expected = IllegalArgumentException::class)
  fun `file does not exist`() {
    assertExpectedProblems(File("does-not-exist.zip"), emptyList())
  }

  @Test
  fun `invalid file extension`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertExpectedProblems(incorrect, listOf(createIncorrectEduPluginFile(incorrect.name)))
  }

  @Test
  fun `language is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified(LANGUAGE)) { language = null }
    checkInvalidPlugin(PropertyNotSpecified(LANGUAGE)) { language = "" }
    checkInvalidPlugin(PropertyNotSpecified(LANGUAGE)) { language = "\n" }
  }

  @Test
  fun `incorrect language specified`() {
    val incorrectLanguage = "en"
    checkInvalidPlugin(UnsupportedLanguage(incorrectLanguage)) { language = incorrectLanguage }
  }

  @Test
  fun `incorrect programming language specified`() {
    val incorrectLanguage = "Kotlin"
    checkInvalidPlugin(UnsupportedProgrammingLanguage) { programmingLanguage = incorrectLanguage }
  }

  @Test
  fun `programming language is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified(PROGRAMMING_LANGUAGE)) { programmingLanguage = null }
    checkInvalidPlugin(PropertyNotSpecified(PROGRAMMING_LANGUAGE)) { programmingLanguage = "" }
    checkInvalidPlugin(PropertyNotSpecified(PROGRAMMING_LANGUAGE)) { programmingLanguage = "\n" }
  }

  @Test
  fun `title is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("title")) { title = null }
    checkInvalidPlugin(PropertyNotSpecified("title")) { title = "" }
    checkInvalidPlugin(PropertyNotSpecified("title")) { title = "\n" }
  }

  @Test
  fun `programming_language is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("programming_language")) { programming_language = null }
    checkInvalidPlugin(PropertyNotSpecified("programming_language")) { programming_language = "" }
    checkInvalidPlugin(PropertyNotSpecified("programming_language")) { programming_language = "\n" }
  }

  @Test
  fun `version is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("version")) { version = null }
    checkInvalidPlugin(PropertyNotSpecified("version")) { version = "" }
    checkInvalidPlugin(PropertyNotSpecified("version")) { version = "\n" }
  }

  @Test
  fun `title is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified(TITLE)) { title = null }
    checkInvalidPlugin(PropertyNotSpecified(TITLE)) { title = "" }
    checkInvalidPlugin(PropertyNotSpecified(TITLE)) { title = "\n" }
  }

  @Test
  fun `summary is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified(SUMMARY)) { summary = null }
    checkInvalidPlugin(PropertyNotSpecified(SUMMARY)) { summary = "" }
    checkInvalidPlugin(PropertyNotSpecified(SUMMARY)) { summary = "\n" }
  }

  @Test
  fun `no items provided`() {
    checkInvalidPlugin(PropertyNotSpecified(ITEMS)) { items = null }
    checkInvalidPlugin(PropertyNotSpecified(ITEMS)) { items = emptyList() }
  }

  @Test
  fun `broken edu plugin version`() {
    var badVersion = "3.7-2019.3"
    checkInvalidPlugin(InvalidVersionError(badVersion)) { version = badVersion }
    badVersion = "3.7"
    checkInvalidPlugin(InvalidVersionError(badVersion)) { version = badVersion }
    badVersion = "3.7-2019.3-5266-3.7"
    checkInvalidPlugin(InvalidVersionError(badVersion)) { version = badVersion }
  }

  @Test
  fun `edu plugin version is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified(EDU_PLUGIN_VERSION)) { version = "" }
    checkInvalidPlugin(PropertyNotSpecified(EDU_PLUGIN_VERSION)) { version = null }
    checkInvalidPlugin(PropertyNotSpecified(EDU_PLUGIN_VERSION)) { version = "\n" }
  }

  private fun checkInvalidPlugin(problem: PluginProblem, descriptor: EduPluginJsonBuilder.() -> Unit) {
    val pluginFile = buildZipFile(temporaryFolder.newFile("course.zip")) {
      file(EduPluginManager.DESCRIPTOR_NAME) {
        val builder = perfectEduPluginBuilder
        builder.descriptor()
        builder.asString()
      }
    }
    assertExpectedProblems(pluginFile, listOf(problem))
  }
}