package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import kotlin.reflect.KClass

sealed class RemappedLevel

object IgnoredLevel : RemappedLevel()
data class StandardLevel(val originalLevel: PluginProblem.Level) : RemappedLevel()

inline fun <reified T : PluginProblem> error(): Map<KClass<*>, RemappedLevel> {
  return mapOf(T::class to StandardLevel(PluginProblem.Level.ERROR))
}

inline fun <reified T : PluginProblem> unacceptableWarning(): Map<KClass<*>, RemappedLevel> {
  return mapOf(T::class to StandardLevel(PluginProblem.Level.UNACCEPTABLE_WARNING))
}

inline fun <reified T : PluginProblem> warning(): Map<KClass<*>, RemappedLevel> {
  return mapOf(T::class to StandardLevel(PluginProblem.Level.WARNING))
}

inline fun <reified T : PluginProblem> ignore(): Map<KClass<*>, RemappedLevel> {
  return mapOf(T::class to IgnoredLevel)
}