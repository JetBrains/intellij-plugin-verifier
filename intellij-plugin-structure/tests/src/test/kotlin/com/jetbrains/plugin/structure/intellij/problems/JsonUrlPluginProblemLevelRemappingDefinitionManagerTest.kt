package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.net.URL

class JsonUrlPluginProblemLevelRemappingDefinitionManagerTest {
  @Test
  fun `plugin problems are loaded from JSON`() {
    val definitionManager = JsonUrlPluginProblemLevelRemappingDefinitionManager(PLUGIN_PROBLEMS_FILE_NAME.asUrl())
    val levelRemappings =  definitionManager.load()

    MatcherAssert.assertThat(levelRemappings.size, `is`(2))

    val existingPluginLevelRemapping = levelRemappings["existing-plugin"]
    Assert.assertNotNull(existingPluginLevelRemapping)
    existingPluginLevelRemapping?.let {
      val ignoredProblems = it.findProblemsByLevel(IgnoredLevel)
      MatcherAssert.assertThat(ignoredProblems.size, `is`(3))
    }

    val newPluginProblemSet = levelRemappings["new-plugin"]
    Assert.assertNotNull(newPluginProblemSet)
    newPluginProblemSet?.let {
      val errors = it.findProblemsByLevel(StandardLevel(PluginProblem.Level.ERROR))
      MatcherAssert.assertThat(errors.size, `is`(3))
    }
  }

  @Test
  fun `plugin problems are loaded from JSON in classpath`() {
    val levelMappingManager = levelRemappingFromClassPathJson()
    val levelRemappings =  levelMappingManager.load()
    MatcherAssert.assertThat(levelRemappings.size, `is`(2))
  }

  @Test
  fun `plugin problems are loaded from unavailable JSON`() {
    Assert.assertThrows(IOException::class.java) {
      val pluginProblemsLoader = JsonUrlPluginProblemLevelRemappingDefinitionManager("nonexistent-file.json".asUrl())
      pluginProblemsLoader.load()
    }
  }

  private fun String.asUrl(): URL = PluginProblemsLoader::class.java.getResource(this)
    ?: throw IOException("JSON URL cannot be found at <$this>")
}