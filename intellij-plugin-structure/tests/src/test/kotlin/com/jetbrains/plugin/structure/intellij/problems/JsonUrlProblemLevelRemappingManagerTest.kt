package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.URL

class JsonUrlProblemLevelRemappingManagerTest {
  @Test
  fun `plugin problems are loaded from JSON`() {
    val definitionManager = JsonUrlProblemLevelRemappingManager(PLUGIN_PROBLEMS_FILE_NAME.asUrl())
    val levelRemappings =  definitionManager.load()

    assertThat(levelRemappings.size, `is`(3))

    val existingPluginLevelRemapping = levelRemappings["existing-plugin"]
    Assert.assertNotNull(existingPluginLevelRemapping)
    existingPluginLevelRemapping?.let {
      val ignoredProblems = it.findProblemsByLevel(IgnoredLevel)
      assertThat(ignoredProblems.size, `is`(4))
    }

    val newPluginProblemSet = levelRemappings["new-plugin"]
    Assert.assertNotNull(newPluginProblemSet)
    newPluginProblemSet?.let {
      val errors = it.findProblemsByLevel(StandardLevel(PluginProblem.Level.ERROR))
      assertThat(errors.size, `is`(3))
    }
  }

  @Test
  fun `plugin problems are loaded from JSON in classpath`() {
    val levelMappingManager = levelRemappingFromClassPathJson()
    val levelRemappings =  levelMappingManager.load()
    assertThat(levelRemappings.size, `is`(3))
  }

  @Test
  fun `plugin problems are loaded from unavailable JSON`() {
    Assert.assertThrows(IOException::class.java) {
      val pluginProblemsLoader = JsonUrlProblemLevelRemappingManager("nonexistent-file.json".asUrl())
      pluginProblemsLoader.load()
    }
  }

  @Test
  fun `plugin problems are loaded from JSON in classpath with nonexistent plugin problem name`() {
    val levelMappingManager = JsonUrlProblemLevelRemappingManager("plugin-problems-incorrect-class.json".asUrl())
    val levelRemappings =  levelMappingManager.load()
    assertThat(levelRemappings.size, `is`(1))
    val remappingName = "existing-plugin"
    val remapping = levelRemappings[remappingName]
    if (remapping == null) {
      throw AssertionError("Remapping '$remappingName' not found")
    }
    assertTrue(remapping.isEmpty())
  }

  @Test
  fun `plugin problems are loaded from JSON in classpath with short plugin problem names`() {
    val levelMappingManager = JsonUrlProblemLevelRemappingManager("plugin-problems-short-names.json".asUrl())
    val remappingDefinitions =  levelMappingManager.load()
    assertThat(remappingDefinitions.size, `is`(1))
    val remappingName = "existing-plugin"
    val remapping = remappingDefinitions[remappingName] ?: throw AssertionError("Remapping '$remappingName' not found")
    assertThat(remapping.size, `is`(2))
  }

  private fun String.asUrl(): URL = JsonUrlProblemLevelRemappingManager::class.java.getResource(this)
    ?: throw IOException("JSON URL cannot be found at <$this>")
}