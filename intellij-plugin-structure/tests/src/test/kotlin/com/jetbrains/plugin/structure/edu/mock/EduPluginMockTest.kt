package com.jetbrains.plugin.structure.edu.mock

import com.jetbrains.plugin.structure.base.problems.PluginProblem
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
    assertEquals(false, plugin.isPrivate)
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

    assertEquals(
      listOf(EduTask("task1", taskType = "edu"), EduTask("task2", taskType = "choice"), EduTask("task3", taskType = "choice")),
      eduStat.tasksByLessons[eduStat.lessons[0]]
    )
    assertEquals(
      listOf(EduTask("task1", taskType = "edu"), EduTask("task2", taskType = "choice"), EduTask("task3", taskType = "choice")),
      eduStat.tasksByLessons[eduStat.lessons[1]]
    )
    assertEquals(
      listOf(EduTask("task1", taskType = "theory"), EduTask("task2", taskType = "ide"), EduTask("task3", taskType = "choice")),
      eduStat.tasksByLessons[eduStat.lessons[2]]
    )
  }

  @Test
  fun `parse edu plugin stat framework lesson`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file(EduPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("course_stat_framework_lesson")
      }
      file("courseIcon.svg", iconTestContent)
    }
    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)
    val plugin = pluginCreationSuccess.plugin

    assertEquals("Reinforcement Learning Maze Solver", plugin.pluginName)
    assertEquals(false, plugin.isPrivate)
    val eduStat = plugin.eduStat
    assertNotNull(eduStat)
    assertEquals(3, eduStat!!.lessons.size)
    assertEquals(0, eduStat.sections.size)
    assertEquals("About", eduStat.lessons[0])
    assertEquals("Theory", eduStat.lessons[1])
    assertEquals("Practice", eduStat.lessons[2])

    assertEquals(
      listOf(EduTask("task1", taskType = "edu"), EduTask("task2", taskType = "choice"), EduTask("task3", taskType = "choice")),
      eduStat.tasksByLessons[eduStat.lessons[0]]
    )
    assertEquals(
      listOf(EduTask("task1", taskType = "edu"), EduTask("task2", taskType = "choice"), EduTask("task3", taskType = "choice")),
      eduStat.tasksByLessons[eduStat.lessons[1]]
    )
    assertEquals(
      listOf(EduTask("task1", taskType = "theory"), EduTask("task2", taskType = "ide"), EduTask("task3", taskType = "choice")),
      eduStat.tasksByLessons[eduStat.lessons[2]]
    )
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
    assertEquals(true, plugin.isPrivate)
    val eduStat = plugin.eduStat
    assertNotNull(eduStat)
    assertEquals(3, eduStat!!.lessons.size)
    assertEquals(2, eduStat.sections.size)
    val section1Stat = eduStat.sections[0]
    assertNotNull(section1Stat)
    assertEquals(1, section1Stat.items.size)
    assertEquals(listOf("lesson1 in section1"), section1Stat.items)
    val section2Stat = eduStat.sections[1]
    assertNotNull(section2Stat)
    assertEquals(2, section2Stat.items.size)
    assertEquals(listOf("lesson1 in section2", "lesson2 in section2"), section2Stat.items)
    assertEquals(4, eduStat.tasks.size)
    assertEquals(3, eduStat.tasks[TaskType.EDU.id])
    assertEquals(1, eduStat.tasks[TaskType.CHOICE.id])
    assertEquals(1, eduStat.tasks[TaskType.THEORY.id])
    assertEquals(1, eduStat.tasks[TaskType.IDE.id])
    assertEquals(0, eduStat.tasks[TaskType.OUTPUT.id] ?: 0)

    assertEquals(4, eduStat.countInteractiveChallenges())
    assertEquals(1, eduStat.countQuizzes())
    assertEquals(1, eduStat.countTheoryTasks())

    assertEquals(
      listOf(
        EduTask("task1 in lesson1 in section1", taskType = "edu"),
        EduTask("task2 in lesson1 in section1", taskType = "theory"),
        EduTask("task3 in lesson1 in section1", taskType = "ide"),
        EduTask("task4 in lesson1 in section1", taskType = "choice")
      ),
      eduStat.tasksByLessons[eduStat.lessons[0]]
    )
    assertEquals(
      listOf(
        EduTask("task1 in lesson1 in section2", taskType = "edu")
      ),
      eduStat.tasksByLessons[eduStat.lessons[1]]
      )
    assertEquals(
      listOf(
        EduTask("task1 in lesson2 in section2", taskType = "edu")
      ),
      eduStat.tasksByLessons[eduStat.lessons[2]]
      )
  }

  @Test
  fun `parse edu plugin stat with sections order test`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file(EduPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("course_stat_sections_rustling")
      }
      file("courseIcon.svg", iconTestContent)
    }
    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)
    val plugin = pluginCreationSuccess.plugin

    assertEquals("Rustlings", plugin.pluginName)
    val eduStat = plugin.eduStat
    assertNotNull(eduStat)
    val sectionNames = listOf("Introduction", "Common Programming Concepts", "Understanding Ownership",
    "Structs", "Enums", "Modules and Macros", "Common Collections", "Type Conversions",
    "Recoverable and Unrecoverable Errors", "Generic Types, Traits and Lifetime",
    "Writing Automated Tests", "Iterators and Closures", "Fearless Concurrency")
    assertEquals(13, eduStat!!.sections.size)
    assertEquals(sectionNames, eduStat.sections.map { it.title })
  }

  @Test
  fun `parse edu plugin with id`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file(EduPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("course_with_id")
      }
      file("courseIcon.svg", iconTestContent)
    }

    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)
    val plugin = pluginCreationSuccess.plugin
    assertEquals("Python Course", plugin.pluginName)
    assertEquals("Python course.\nCreated: May 6, 2020, 11:21:51 AM.", plugin.description)
    assertEquals("1.1", plugin.pluginVersion)
    assertEquals("JetBrains s.r.o.", plugin.vendor)
    assertEquals("en", plugin.language)
    assertEquals("Python", plugin.programmingLanguageId)
    assertEquals("2.7", plugin.programmingLanguageVersion)
    assertEquals("unittest", plugin.environment)
    assertEquals("new_id", plugin.pluginId)
    assertEquals(1, plugin.eduStat!!.lessons.size)
    assertEquals("lesson1", plugin.eduStat!!.lessons[0])
    assertEquals(false, plugin.isPrivate)
    assertEquals(iconTestContent, String(plugin.icons.single().content))
    assertTrue(pluginCreationSuccess.warnings.isEmpty())
  }

  @Test
  fun `parse edu plugin with obsolete programming lang`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file(EduPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("course_obsolete_programming_lang")
      }
      file("courseIcon.svg", iconTestContent)
    }

    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)
    val plugin = pluginCreationSuccess.plugin
    assertEquals("Python Course", plugin.pluginName)
    assertEquals("Python course.\nCreated: May 6, 2020, 11:21:51 AM.", plugin.description)
    assertEquals("1.1", plugin.pluginVersion)
    assertEquals("JetBrains s.r.o.", plugin.vendor)
    assertEquals("en", plugin.language)
    assertEquals("Test", plugin.programmingLanguage)
    assertNull(plugin.programmingLanguageId)
    assertNull(plugin.programmingLanguageVersion)
    assertEquals("unittest", plugin.environment)
    assertEquals("Python Course_JetBrains s.r.o._Test", plugin.pluginId)
    assertEquals(1, plugin.eduStat!!.lessons.size)
    assertEquals("lesson1", plugin.eduStat!!.lessons[0])
    assertEquals(false, plugin.isPrivate)
    assertEquals(iconTestContent, String(plugin.icons.single().content))
    assertTrue(pluginCreationSuccess.warnings.isEmpty())
  }

  @Test
  fun `parse edu plugin with obsolete programming lang and version`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file(EduPluginManager.DESCRIPTOR_NAME) {
        getMockPluginJsonContent("course_obsolete_programming_lang_and_version")
      }
      file("courseIcon.svg", iconTestContent)
    }

    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)
    val plugin = pluginCreationSuccess.plugin
    assertEquals("Python Course", plugin.pluginName)
    assertEquals("Python course.\nCreated: May 6, 2020, 11:21:51 AM.", plugin.description)
    assertEquals("1.1", plugin.pluginVersion)
    assertEquals("JetBrains s.r.o.", plugin.vendor)
    assertEquals("en", plugin.language)
    assertEquals("Test 12", plugin.programmingLanguage)
    assertNull(plugin.programmingLanguageId)
    assertNull(plugin.programmingLanguageVersion)
    assertEquals("unittest", plugin.environment)
    assertEquals("Python Course_JetBrains s.r.o._Test", plugin.pluginId)
    assertEquals(1, plugin.eduStat!!.lessons.size)
    assertEquals("lesson1", plugin.eduStat!!.lessons[0])
    assertEquals(false, plugin.isPrivate)
    assertEquals(iconTestContent, String(plugin.icons.single().content))
    assertTrue(pluginCreationSuccess.warnings.isEmpty())
  }

  @Test
  fun `check presentable name for sections and lessons with custom name`() {
    val lesson = EduItem(
      type = ItemType.LESSON.id,
      title = "lesson",
      customName = "lesson custom name",
    )
    val section = EduItem(
      type = ItemType.SECTION.id,
      title = "section",
      customName = "section custom name",
      items = listOf(lesson)
    )

    val result = createPluginSuccessfully(buildEduPlugin(temporaryFolder.newFolder().resolve("course.zip")) {
      items = listOf(section)
    })
    assertNotNull("Edu stats were not parsed", result.plugin.eduStat)
    val stats = result.plugin.eduStat!!

    assertEquals(
      "Section's title should be equal to custom name if it's present",
      section.customName, stats.sections.firstOrNull()?.title
    )

    assertEquals("Lesson's title should be equal to custom name if it's present",
      lesson.customName, stats.lessons.firstOrNull()
    )
  }

  @Test
  fun `check presentable name for sections and lessons without custom name`() {
    val lesson = EduItem(
      type = ItemType.LESSON.id,
      title = "lesson",
      customName = " ",
    )
    val section = EduItem(
      type = ItemType.SECTION.id,
      title = "section",
      customName = " ",
      items = listOf(lesson)
    )

    val result = createPluginSuccessfully(buildEduPlugin(temporaryFolder.newFolder().resolve("course.zip")) {
      items = listOf(section)
    })
    assertNotNull("Edu stats were not parsed", result.plugin.eduStat)
    val stats = result.plugin.eduStat!!

    assertEquals(
      "Section's title should be equal to title if custom name is blank",
      section.title, stats.sections.firstOrNull()?.title
    )

    assertEquals(
      "Lesson's title should be equal to title if custom name is blank",
      lesson.title, stats.lessons.firstOrNull()
    )
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
    assertEquals("Python", plugin.programmingLanguageId)
    assertEquals("2.7", plugin.programmingLanguageVersion)
    assertEquals("unittest", plugin.environment)
    assertEquals("Python Course_JetBrains s.r.o._Python", plugin.pluginId)
    assertEquals(1, plugin.eduStat!!.lessons.size)
    assertEquals("lesson1", plugin.eduStat!!.lessons[0])
    assertEquals(false, plugin.isPrivate)
    assertEquals(iconTestContent, String(plugin.icons.single().content))
  }
}
