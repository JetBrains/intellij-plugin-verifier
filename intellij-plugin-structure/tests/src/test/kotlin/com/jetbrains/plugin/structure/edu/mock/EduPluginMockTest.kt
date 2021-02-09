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
    assertEquals(0, eduStat.sections.size)
    assertEquals(4, eduStat.tasks.size)
    assertEquals(2, eduStat.tasks[TaskType.EDU.id])
    assertEquals(5, eduStat.tasks[TaskType.CHOICE.id])
    assertEquals(1, eduStat.tasks[TaskType.THEORY.id])
    assertEquals(1, eduStat.tasks[TaskType.IDE.id])
    assertEquals(0, eduStat.tasks[TaskType.OUTPUT.id] ?: 0)

    assertEquals(3, eduStat.countInteractiveChallenges())
    assertEquals(5, eduStat.countQuizzes())
    assertEquals(1, eduStat.countTheoryTasks())
  }

  @Test
  fun `parse edu plugin stat with sections test`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file(EduPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("course_stat_sections")
      }
      file("courseIcon.svg", iconTestContent)
    }
    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)
    val plugin = pluginCreationSuccess.plugin

    assertEquals("Python Course", plugin.pluginName)
    val eduStat = plugin.eduStat
    assertNotNull(eduStat)
    assertEquals(3, eduStat!!.lessons.size)
    assertEquals(2, eduStat.sections.size)
    val section1Stat = eduStat.sections["section1"]
    assertNotNull(section1Stat)
    assertEquals(1, section1Stat!!.size)
    assertEquals(listOf("lesson1 in section1"), section1Stat)
    val section2Stat = eduStat.sections["section2"]
    assertNotNull(section2Stat)
    assertEquals(2, section2Stat!!.size)
    assertEquals(listOf("lesson1 in section2", "lesson2 in section2"), section2Stat)
    assertEquals(4, eduStat.tasks.size)
    assertEquals(3, eduStat.tasks[TaskType.EDU.id])
    assertEquals(1, eduStat.tasks[TaskType.CHOICE.id])
    assertEquals(1, eduStat.tasks[TaskType.THEORY.id])
    assertEquals(1, eduStat.tasks[TaskType.IDE.id])
    assertEquals(0, eduStat.tasks[TaskType.OUTPUT.id] ?: 0)

    assertEquals(4, eduStat.countInteractiveChallenges())
    assertEquals(1, eduStat.countQuizzes())
    assertEquals(1, eduStat.countTheoryTasks())
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
    assertEquals("1.1", plugin.pluginVersion)
    assertEquals("JetBrains s.r.o.", plugin.vendor)
    assertEquals("en", plugin.language)
    assertEquals("Python", plugin.programmingLanguage)
    assertEquals("unittest", plugin.environment)
    assertEquals("Python Course_JetBrains s.r.o._Python", plugin.pluginId)
    assertEquals(1, plugin.eduStat!!.lessons.size)
    assertEquals("lesson1", plugin.eduStat!!.lessons[0])
    assertEquals(iconTestContent, String(plugin.icons.single().content))
  }
}
