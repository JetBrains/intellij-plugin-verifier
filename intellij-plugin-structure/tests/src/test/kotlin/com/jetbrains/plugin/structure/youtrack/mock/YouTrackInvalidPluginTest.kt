package com.jetbrains.plugin.structure.youtrack.mock

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.youtrack.YouTrackPlugin
import com.jetbrains.plugin.structure.youtrack.YouTrackPluginManager
import com.jetbrains.plugin.structure.youtrack.YouTrackVersionUtils
import com.jetbrains.plugin.structure.youtrack.bean.YouTrackAppFields
import com.jetbrains.plugin.structure.youtrack.bean.YouTrackAppManifest
import com.jetbrains.plugin.structure.youtrack.bean.YouTrackAppWidget
import com.jetbrains.plugin.structure.youtrack.problems.*
import com.jetbrains.plugin.structure.youtrack.validateYouTrackManifest
import org.junit.Assert
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

class YouTrackInvalidPluginTest(fileSystemType: FileSystemType) : BasePluginManagerTest<YouTrackPlugin, YouTrackPluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path): YouTrackPluginManager {
    return YouTrackPluginManager.createManager(extractDirectory)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `file does not exist`() {
    assertProblematicPlugin(Paths.get("does-not-exist.zip"), emptyList())
  }

  @Test
  fun `invalid file extension`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertProblematicPlugin(incorrect, listOf(createIncorrectYouTrackPluginFileError(incorrect.simpleName)))
  }

  @Test
  fun `manifest json not found in directory`() {
    val pluginFile = buildDirectory(temporaryFolder.newFolder("app")) {
    }
    assertProblematicPlugin(pluginFile, listOf(PluginDescriptorIsNotFound(YouTrackPluginManager.DESCRIPTOR_NAME)))
  }

  @Test
  fun `manifest json not found in zip`() {
    val pluginFile = buildZipFile(temporaryFolder.newFolder().resolve("app.zip")) {
    }
    assertProblematicPlugin(pluginFile, listOf(PluginDescriptorIsNotFound(YouTrackPluginManager.DESCRIPTOR_NAME)))
  }

  @Test
  fun `invalid app name`() {
    checkInvalidPlugin(ManifestPropertyNotSpecified(YouTrackAppFields.Manifest.NAME)) { it.copy(name = null) }
    checkInvalidPlugin(AppNameIsBlank()) { it.copy(name = "") }
    checkInvalidPlugin(UnsupportedSymbolsAppNameProblem()) { it.copy(name = "hello world") }
  }

  @Test
  fun `name contains unallowed symbols`() {
    for (i in 1..10) {
      val name = getRandomNotAllowedNameSymbols(i)
      checkInvalidPlugin(InvalidPluginName("manifest.json", name)) {
        it.copy(title = name)
      }
    }
  }

  @Test
  fun `name contains only allowed symbols`() {
    for (i in 1..10) {
      val name = getRandomAllowedNameSymbols(i)
      checkValidPlugin {
        it.copy(name = name)
      }
    }
  }

  @Test
  fun `invalid app title`() {
    checkInvalidPlugin(ManifestPropertyNotSpecified(YouTrackAppFields.Manifest.TITLE)) { it.copy(title = null) }
    checkInvalidPlugin(ManifestPropertyNotSpecified(YouTrackAppFields.Manifest.TITLE)) { it.copy(title = "") }
  }

  @Test
  fun `app title is too long`() {
    var longTitle = "a"
    repeat(MAX_NAME_LENGTH) { longTitle += "a" }
    val expectedProblem = TooLongPropertyValue("manifest.json", YouTrackAppFields.Manifest.TITLE, longTitle.length, MAX_NAME_LENGTH)
    checkInvalidPlugin(expectedProblem) { it.copy(title = longTitle) }
  }

  @Test
  fun `invalid app description`() {
    checkInvalidPlugin(ManifestPropertyNotSpecified(YouTrackAppFields.Manifest.DESCRIPTION)) { it.copy(description = null) }
    checkInvalidPlugin(ManifestPropertyNotSpecified(YouTrackAppFields.Manifest.DESCRIPTION)) { it.copy(description = "") }
  }

  @Test
  fun `invalid app version`() {
    checkInvalidPlugin(ManifestPropertyNotSpecified(YouTrackAppFields.Manifest.VERSION)) { it.copy(version = null) }
    checkInvalidPlugin(ManifestPropertyNotSpecified(YouTrackAppFields.Manifest.VERSION)) { it.copy(version = "") }
  }

  @Test
  fun `app changeNotes is too long`() {
    var longChangeNotes = "a"
    repeat(MAX_CHANGE_NOTES_LENGTH) { longChangeNotes += "a" }
    val expectedProblem = TooLongPropertyValue("manifest.json", YouTrackAppFields.Manifest.NOTES, longChangeNotes.length, MAX_CHANGE_NOTES_LENGTH)
    checkInvalidPlugin(expectedProblem) { it.copy(changeNotes = longChangeNotes) }
  }

  @Test
  fun `null widget key`() {
    val widgets = listOf(widget.copy(key = null), widget)
    checkInvalidPlugin(WidgetKeyNotSpecified()) { it.copy(widgets = widgets) }
  }

  @Test
  fun `invalid widget key`() {
    val widgets = listOf(widget.copy(key = "AAA"), widget.copy(key = "???"), widget.copy("a.b_c-d~1"), widget.copy("a.b_c-d123"))
    checkInvalidPlugin(
      UnsupportedSymbolsWidgetKeyProblem("AAA"),
      UnsupportedSymbolsWidgetKeyProblem("???"),
      UnsupportedSymbolsWidgetKeyProblem("a.b_c-d~1")
    ) { it.copy(widgets = widgets) }
  }

  @Test
  fun `widget key is not unique`() {
    val widgets = listOf(widget.copy(key = "a"), widget.copy(key = "b"), widget.copy("a"))
    checkInvalidPlugin(WidgetKeyIsNotUnique()) { it.copy(widgets = widgets) }
  }

  @Test
  fun `invalid widget indexPath`() {
    val widgets = listOf(widget.copy(key = "1", indexPath = null), widget.copy(key = "2", indexPath = null), widget)
    checkInvalidPlugin(
      WidgetManifestPropertyNotSpecified(YouTrackAppFields.Widget.INDEX_PATH, "1"),
      WidgetManifestPropertyNotSpecified(YouTrackAppFields.Widget.INDEX_PATH, "2")
    ) { it.copy(widgets = widgets) }
  }

  @Test
  fun `invalid widget extensionPoint`() {
    val widgets = listOf(widget.copy(key = "1", extensionPoint = null), widget.copy(key = "2", extensionPoint = null), widget)
    checkInvalidPlugin(
      WidgetManifestPropertyNotSpecified(YouTrackAppFields.Widget.EXTENSION_POINT, "1"),
      WidgetManifestPropertyNotSpecified(YouTrackAppFields.Widget.EXTENSION_POINT, "2")
    ) { it.copy(widgets = widgets) }
  }

  @Test
  fun `invalid youtrack versions`() {
    checkInvalidPlugin(
      InvalidSemverFormat(
        descriptorPath = YouTrackPluginManager.DESCRIPTOR_NAME,
        versionName = YouTrackAppFields.Manifest.SINCE,
        version = "123"
      ),
      InvalidSemverFormat(
        descriptorPath = YouTrackPluginManager.DESCRIPTOR_NAME,
        versionName = YouTrackAppFields.Manifest.UNTIL,
        version = "456"
      )
    ) { it.copy(minYouTrackVersion = "123", maxYouTrackVersion = "456") }
  }

  @Test
  fun `invalid youtrack versions range`() {
    checkInvalidPlugin(
      InvalidVersionRange(
        descriptorPath = YouTrackPluginManager.DESCRIPTOR_NAME,
        since = "123.12.1",
        until = "12.12.1"
      )
    ) { it.copy(minYouTrackVersion = "123.12.1", maxYouTrackVersion = "12.12.1") }
  }

  @Test
  fun `invalid youtrack version`() {
    checkInvalidPlugin(
      SemverComponentLimitExceeded(
        descriptorPath = YouTrackPluginManager.DESCRIPTOR_NAME,
        componentName = "major",
        versionName = YouTrackAppFields.Manifest.SINCE,
        version = "3001.1.2",
        limit = YouTrackVersionUtils.MAX_MAJOR_VALUE
      ),
      SemverComponentLimitExceeded(
        descriptorPath = YouTrackPluginManager.DESCRIPTOR_NAME,
        componentName = "major",
        versionName = YouTrackAppFields.Manifest.UNTIL,
        version = "3001.1.2",
        limit = YouTrackVersionUtils.MAX_MAJOR_VALUE
      )
    ) { it.copy(minYouTrackVersion = "3001.1.2", maxYouTrackVersion = "3001.1.2") }

    checkInvalidPlugin(
      SemverComponentLimitExceeded(
        descriptorPath = YouTrackPluginManager.DESCRIPTOR_NAME,
        componentName = "minor",
        versionName = YouTrackAppFields.Manifest.SINCE,
        version = "2022.101.2",
        limit = YouTrackVersionUtils.VERSION_MINOR_LENGTH - 1
      )
    ) { it.copy(minYouTrackVersion =  "2022.101.2") }
    checkInvalidPlugin(
      SemverComponentLimitExceeded(
        descriptorPath = YouTrackPluginManager.DESCRIPTOR_NAME,
        componentName = "minor",
        versionName = YouTrackAppFields.Manifest.UNTIL,
        version = "2022.101.2",
        limit = YouTrackVersionUtils.VERSION_MINOR_LENGTH - 1
      )
    ) { it.copy(maxYouTrackVersion =  "2022.101.2") }

    checkInvalidPlugin(
      SemverComponentLimitExceeded(
        descriptorPath = YouTrackPluginManager.DESCRIPTOR_NAME,
        componentName = "patch",
        versionName = YouTrackAppFields.Manifest.SINCE,
        version = "2022.2.1000001",
        limit = YouTrackVersionUtils.VERSION_PATCH_LENGTH - 1
      )
    ) { it.copy(minYouTrackVersion = "2022.2.1000001") }

    checkInvalidPlugin(
      SemverComponentLimitExceeded(
        descriptorPath = YouTrackPluginManager.DESCRIPTOR_NAME,
        componentName = "patch",
        versionName = YouTrackAppFields.Manifest.UNTIL,
        version = "2022.2.1000001",
        limit = YouTrackVersionUtils.VERSION_PATCH_LENGTH - 1
      )
    ) { it.copy(maxYouTrackVersion = "2022.2.1000001") }

    checkValidPlugin { it.copy(minYouTrackVersion = "2024.99.999999", maxYouTrackVersion = "2024.99.999999") }
  }

  private fun checkInvalidPlugin(vararg expectedProblems: PluginProblem, modify: (YouTrackAppManifest) -> YouTrackAppManifest) {
    val manifestJson = getMockPluginFileContent("manifest.json")
    val manifest = modify(jacksonObjectMapper().readValue(manifestJson, YouTrackAppManifest::class.java))
    Assert.assertEquals(expectedProblems.toList(), validateYouTrackManifest(manifest))
  }

  private fun checkValidPlugin(modify: (YouTrackAppManifest) -> YouTrackAppManifest) {
    val manifestJson = getMockPluginFileContent("manifest.json")
    val manifest = modify(jacksonObjectMapper().readValue(manifestJson, YouTrackAppManifest::class.java))
    Assert.assertEquals(emptyList<PluginProblem>(), validateYouTrackManifest(manifest))
  }

  private val widget: YouTrackAppWidget
    get() {
      val widgetJson = getMockPluginFileContent("widget.json")
      return jacksonObjectMapper().readValue(widgetJson, YouTrackAppWidget::class.java)
    }

}