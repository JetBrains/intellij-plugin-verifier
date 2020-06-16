package com.jetbrains.plugin.structure.edu.mock

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.plugin.structure.edu.EduPlugin
import com.jetbrains.plugin.structure.edu.EduPluginManager
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
    checkInvalidPlugin(PropertyNotSpecified("language")) { language = null }
    checkInvalidPlugin(PropertyNotSpecified("language")) { language = "" }
    checkInvalidPlugin(PropertyNotSpecified("language")) { language = "\n" }
  }

  @Test
  fun `summary is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified("summary")) { summary = null }
    checkInvalidPlugin(PropertyNotSpecified("summary")) { summary = "" }
    checkInvalidPlugin(PropertyNotSpecified("summary")) { summary = "\n" }
  }

  @Test
  fun `too big hub zip file`() {
    val tooBigSize = 301 * 1024 * 1024
    val pluginFile = buildZipFile(temporaryFolder.newFile("course.zip")) {
      file(EduPluginManager.DESCRIPTOR_NAME) { testJsonFileForBaseFields }
      file("bigFile.bin", ByteArray(tooBigSize))
    }
    assertExpectedProblems(pluginFile, listOf(PluginFileSizeIsTooLarge(300 * 1024 * 1024)))
  }


  private fun checkInvalidPlugin(problem: PluginProblem, destructor: EduPluginJsonBuilder.() -> Unit) {
    val pluginFile = buildZipFile(temporaryFolder.newFile("course.zip")) {
      file(EduPluginManager.DESCRIPTOR_NAME) {
        val builder = perfectEduPluginBuilder
        builder.destructor()
        builder.asString()
      }
    }
    assertExpectedProblems(pluginFile, listOf(problem))
  }
}