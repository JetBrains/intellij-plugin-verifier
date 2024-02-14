package com.jetbrains.plugin.structure.intellij.problems

fun interface PluginProblemsLoaderFactory {
  fun getPluginProblemsLoader(): PluginProblemsLoader
}

object ClassPathPluginProblemsLoaderFactory: PluginProblemsLoaderFactory {
  override fun getPluginProblemsLoader(): PluginProblemsLoader {
    return PluginProblemsLoader.fromClassPath()
  }
}