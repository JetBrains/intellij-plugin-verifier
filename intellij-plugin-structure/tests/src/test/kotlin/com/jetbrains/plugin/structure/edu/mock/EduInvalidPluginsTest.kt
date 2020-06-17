package com.jetbrains.plugin.structure.edu.mock

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.plugin.structure.edu.EduPlugin
import com.jetbrains.plugin.structure.edu.EduPluginManager
import com.jetbrains.plugin.structure.edu.LANGUAGE
import com.jetbrains.plugin.structure.edu.TITLE
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