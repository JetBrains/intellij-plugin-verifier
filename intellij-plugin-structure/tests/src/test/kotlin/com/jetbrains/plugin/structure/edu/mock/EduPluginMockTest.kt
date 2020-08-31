package com.jetbrains.plugin.structure.edu.mock

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.edu.*
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Path

class EduPluginMockTest(fileSystemType: FileSystemType) : BasePluginManagerTest<EduPlugin, EduPluginManager>(fileSystemType) {

  private val iconTestContent = "<svg></svg>"

  override fun createManager(extractDirectory: Path): EduPluginManager =
    EduPluginManager.createManager(extractDirectory)

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

  @Test
  fun `parse edu plugin stat test`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file(EduPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("course_stat")
      }
      file("courseIcon.svg", iconTestContent)
    }
    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)
    val plugin = pluginCreationSuccess.plugin

    assertEquals("Python Course", plugin.pluginName)
    val eduStat = plugin.eduStat
    assertNotNull(eduStat)
    assertEquals(3, eduStat!!.lessons.size)
    assertEquals(1, eduStat.sections.size)
    assertEquals(4, eduStat.tasks.size)
    assertEquals(2, eduStat.tasks[TaskType.EDU.id])
    assertEquals(5, eduStat.tasks[TaskType.CHOICE.id])
    assertEquals(2, eduStat.tasks[TaskType.THEORY.id])
    assertEquals(1, eduStat.tasks[TaskType.IDE.id])
    assertEquals(0, eduStat.tasks[TaskType.OUTPUT.id] ?: 0)

    assertEquals(3, eduStat.countInteractiveChallenges())
    assertEquals(5, eduStat.countQuizzes())
    assertEquals(2, eduStat.countTheoryTasks())
  }

  private fun testMockPluginStructureAndConfiguration(pluginFile: Path) {
    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)

    testMockConfigs(pluginCreationSuccess.plugin)
    testMockWarnings(pluginCreationSuccess.warnings)
  }

  private fun testMockWarnings(problems: List<PluginProblem>) {
    assertTrue(problems.isEmpty())
  }

  private fun testMockConfigs(plugin: EduPlugin) {
    assertEquals("Python Course", plugin.pluginName)
    assertEquals("Python course.\nCreated: May 6, 2020, 11:21:51 AM.", plugin.description)
    assertEquals("JetBrains s.r.o.", plugin.vendor)
    assertEquals("en", plugin.language)
    assertEquals("Python", plugin.programmingLanguage)
    assertEquals("Python Course_JetBrains s.r.o._Python", plugin.pluginId)
    assertEquals("3.7-2019.3-5266", plugin.eduPluginVersion)
    assertEquals(EduPluginVersion("3.7", "2019.3", "5266"),
                 plugin.parsedEduVersion)
    assertEquals(1, plugin.eduStat!!.lessons.size)
    assertEquals("lesson1", plugin.eduStat!!.lessons[0])
    assertEquals(iconTestContent, String(plugin.icons.single().content))
  }
}
