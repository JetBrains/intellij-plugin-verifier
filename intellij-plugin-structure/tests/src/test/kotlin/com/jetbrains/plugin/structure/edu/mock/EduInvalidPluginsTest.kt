package com.jetbrains.plugin.structure.edu.mock

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.edu.*
import com.jetbrains.plugin.structure.edu.bean.Vendor
import com.jetbrains.plugin.structure.edu.problems.InvalidVersionError
import com.jetbrains.plugin.structure.edu.problems.UnsupportedLanguage
import com.jetbrains.plugin.structure.edu.problems.UnsupportedProgrammingLanguage
import com.jetbrains.plugin.structure.edu.problems.createIncorrectEduPluginFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

class EduInvalidPluginsTest(fileSystemType: FileSystemType) : BasePluginManagerTest<EduPlugin, EduPluginManager>(fileSystemType) {
  override fun createManager(extractDirectory: Path): EduPluginManager =
    EduPluginManager.createManager(extractDirectory)

  @Test(expected = IllegalArgumentException::class)
  fun `file does not exist`() {
    assertProblematicPlugin(Paths.get("does-not-exist.zip"), emptyList())
  }

  @Test
  fun `invalid file extension`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertProblematicPlugin(incorrect, listOf(createIncorrectEduPluginFile(incorrect.simpleName)))
  }

  @Test
  fun `language is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified(LANGUAGE)) { language = null }
    checkInvalidPlugin(PropertyNotSpecified(LANGUAGE)) { language = "" }
    checkInvalidPlugin(PropertyNotSpecified(LANGUAGE)) { language = "\n" }
  }

  @Test
  fun `vendor is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified(VENDOR)) { vendor = null }
    checkInvalidPlugin(PropertyNotSpecified(VENDOR)) { vendor = Vendor() }
    checkInvalidPlugin(PropertyNotSpecified(VENDOR)) { vendor = Vendor("") }
    checkInvalidPlugin(PropertyNotSpecified(VENDOR)) { vendor = Vendor("\n") }
  }

  @Test
  fun `incorrect language specified`() {
    val incorrectLanguage = "english"
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
    val pluginFile = buildZipFile(temporaryFolder.newFolder().resolve("course.zip")) {
      file(EduPluginManager.DESCRIPTOR_NAME) {
        val builder = perfectEduPluginBuilder
        builder.descriptor()
        builder.asString()
      }
    }
    assertProblematicPlugin(pluginFile, listOf(problem))
  }
}