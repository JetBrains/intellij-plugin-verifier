package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.intellij.problems.PluginProblemsLoader.PluginProblemLevel.Error
import com.jetbrains.plugin.structure.intellij.problems.PluginProblemsLoader.PluginProblemLevel.Ignored
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.URL


class PluginProblemsLoaderTestLevel {
  @Test
  fun `plugin problems are loaded from JSON`() {
    val pluginProblemsLoader = PluginProblemsLoader("plugin-problems.json".asUrl())
    pluginProblemsLoader.load()

    val collections = pluginProblemsLoader.pluginProblemSetCollection
    assertThat(collections.size, `is`(2));

    val existingPluginProblemSet = collections["existing-plugin"]
    assertNotNull(existingPluginProblemSet)
    existingPluginProblemSet?.let {
      val ignoredProblems = it.problems.filterIsInstance<Ignored>()
      assertThat(ignoredProblems.size, `is`(3))
    }

    val newPluginProblemSet = collections["new-plugin"]
    assertNotNull(newPluginProblemSet)
    newPluginProblemSet?.let {
      val errors = it.problems.filterIsInstance<Error>()
      assertThat(errors.size, `is`(3))
    }
  }

  @Test
  fun `plugin problems are loaded from unavailable JSON`() {
    assertThrows(IOException::class.java) {
      val pluginProblemsLoader = PluginProblemsLoader("nonexistent-file.json".asUrl())
      pluginProblemsLoader.load()
    }
  }

  private fun String.asUrl(): URL = PluginProblemsLoader::class.java.getResource(this)
    ?: throw IOException("JSON URL cannot be found at <$this>")
}