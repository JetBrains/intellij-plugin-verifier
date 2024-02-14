package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.WARNING
import com.jetbrains.plugin.structure.intellij.problems.PluginProblemsLoader.PluginProblemLevel.*
import kotlin.reflect.KClass

private const val INTELLIJ_PROBLEMS_PACKAGE_NAME = "com.jetbrains.plugin.structure.intellij.problems"

class RemappedPluginProblemLevelParser {
  fun parse(pluginProblemMappingDefinition: PluginProblemSet): Map<KClass<*>, RemappedLevel> {
    return pluginProblemMappingDefinition.problems.mapNotNull { levelRemapping ->
      val pluginProblemKClass = resolveClass(levelRemapping.problemId) ?: return@mapNotNull null
      val remappedLevel = when (levelRemapping) {
        is Error -> StandardLevel(ERROR)
        is Warning -> StandardLevel(WARNING)
        is Ignored -> IgnoredLevel
      }
      pluginProblemKClass to remappedLevel
    }.toMap()
  }

  /**
   * Resolves the problem ID to a fully qualified class name.
   *
   * Example: `ForbiddenPluginIdPrefix` to `com.jetbrains.plugin.structure.intellij.problems.ForbiddenPluginIdPrefix`
   */
  private fun resolveClass(problemId: String): KClass<out Any>? {
    val fqn = "$INTELLIJ_PROBLEMS_PACKAGE_NAME.$problemId"
    return runCatching {
      val pluginProblemJavaClass = Class.forName(fqn, false, this.javaClass.getClassLoader())
      val kotlin = pluginProblemJavaClass.kotlin
      kotlin
    }.getOrNull()
  }
}