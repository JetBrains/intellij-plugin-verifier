package com.jetbrains.plugin.structure.intellij.problems

import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.URL


class PluginProblemsLoaderTest {
  @Test
  fun `plugin problems are loaded from JSON`() {
    val pluginProblemsLoader = PluginProblemsLoader("plugin-problems.json".asUrl())
    pluginProblemsLoader.load()
  }

  @Test
  fun `plugin problems are loaded from unavailable JSON`() {
    assertThrows(IOException::class.java) {
      val pluginProblemsLoader = PluginProblemsLoader("nonexistent-file.json".asUrl())
      pluginProblemsLoader.load()
    }
  }

  private fun String.asUrl(): URL {
    val jsonUrl = PluginProblemsLoader::class::java.javaClass.getResource(this)
    if (jsonUrl === null) {
      throw IOException("JSON URL cannot be found at <$this>")
    } else {
      return jsonUrl
    }
  }
}