package com.jetbrains.plugin.structure.intellij

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.telemetry.MutablePluginTelemetry
import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFIED_CLASSES_COUNT
import com.jetbrains.plugin.structure.base.telemetry.UNKNOWN_SIZE
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Duration

private const val HEADER = """
      <id>someId</id>
      <name>someName</name>
      <version>someVersion</version>
      ""<vendor email="vendor.com" url="url">vendor</vendor>""
      <description>this description is looooooooooong enough</description>
      <change-notes>these change-notes are looooooooooong enough</change-notes>
      <idea-version since-build="131.1"/>
    """

class TelemetryTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `successful telemetry test`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
            </idea-plugin>
          """
        }
      }
    }
    with(pluginCreationSuccess.telemetry) {
      assertTrue(archiveFileSize > 0)
      assertTrue(parsingDuration.isZeroOrPositive())
    }
  }

  @Test
  fun `archive file size is not set but retrieved`() {
    val telemetry = MutablePluginTelemetry()
    val archiveFileSize = telemetry.archiveFileSize
    assertEquals(UNKNOWN_SIZE, archiveFileSize)
  }

  @Test
  fun `duration is not set but retrieved`() {
    val telemetry = MutablePluginTelemetry()
    val parsingDuration = telemetry.parsingDuration
    assertNull(parsingDuration)
  }

  @Test
  fun `verified number of classes is not set but retrieved`() {
    val telemetry = MutablePluginTelemetry()
    val verifiedClassesCount = telemetry[PLUGIN_VERIFIED_CLASSES_COUNT]
    assertNull(verifiedClassesCount)
  }


  private fun buildCorrectPlugin(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationSuccess<IdePlugin> {
    val pluginCreationResult = buildIdePlugin(pluginContentBuilder)
    if (pluginCreationResult !is PluginCreationSuccess) {
      fail("This plugin has not been created. Creation failed with error(s).")
    }
    return pluginCreationResult as PluginCreationSuccess
  }

  private fun buildIdePlugin(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar").toPath(), pluginContentBuilder)
    return IdePluginManager.createManager().createPlugin(pluginFile)
  }

  fun Duration?.isZeroOrPositive(): Boolean {
    return if (this != null) !this.isNegative else false
  }
}