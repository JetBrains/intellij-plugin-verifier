package com.jetbrains.plugin.structure.edu.mock

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.edu.EduPlugin
import com.jetbrains.plugin.structure.edu.EduPluginManager
import com.jetbrains.plugin.structure.edu.EduPluginVersion
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File


class EduPluginMockTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()
  private val iconTestContent = "<svg></svg>"

  @Test
  fun `parse base fields edu plugin test`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file(EduPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("course")
      }
      file("courseIcon.svg", iconTestContent)
    }
    testMockPluginStructureAndConfiguration(pluginFile)
  }

  private fun testMockPluginStructureAndConfiguration(pluginFile: File) {
    val pluginCreationResult = EduPluginManager.createManager().createPlugin(pluginFile)
    if (pluginCreationResult is PluginCreationFail) {
      val message = pluginCreationResult.errorsAndWarnings.joinToString(separator = "\n") { it.message }
      Assert.fail(message)
    }
    val pluginCreationSuccess = pluginCreationResult as PluginCreationSuccess

    testMockConfigs(pluginCreationSuccess.plugin)
    testMockWarnings(pluginCreationSuccess.warnings)
  }

  private fun testMockWarnings(problems: List<PluginProblem>) {
    Assert.assertTrue(problems.isEmpty())
  }

  private fun testMockConfigs(plugin: EduPlugin) {
    Assert.assertEquals("Python Course", plugin.pluginName)
    Assert.assertEquals("Python course.\nCreated: May 6, 2020, 11:21:51 AM.", plugin.description)
    Assert.assertEquals("JetBrains s.r.o.", plugin.vendor)
    Assert.assertEquals("en", plugin.language)
    Assert.assertEquals("Python", plugin.programmingLanguage)
    Assert.assertEquals("3.7-2019.3-5266", plugin.eduPluginVersion)
    Assert.assertEquals(EduPluginVersion("3.7", "2019.3", "5266"),
                        plugin.parsedEduVersion)
    Assert.assertEquals(1, plugin.items.size)
    Assert.assertEquals("lesson1", plugin.items[0])
    Assert.assertEquals(iconTestContent, String(plugin.icons.single().content))
  }
}


