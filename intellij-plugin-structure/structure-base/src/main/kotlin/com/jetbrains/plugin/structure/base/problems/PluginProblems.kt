package com.jetbrains.plugin.structure.base.problems

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

private val LOG: Logger = LoggerFactory.getLogger(PluginProblems::class.java)

fun PluginProblem.isReclassified(): Boolean = this is ReclassifiedPluginProblem

/**
 * Unwrap an original problem from a reclassified problem.
 * If this is not a reclassified problem, return itself.
 */
val PluginProblem.unwrapped: PluginProblem
  get() = if (this is ReclassifiedPluginProblem) {
    unwrapped
  } else {
    this
  }

/**
 * Indicate if this problem is an [Invalid Descriptor Problem](InvalidDescriptorProblem).
 * If this is a reclassified problem, the original problem will be unwrapped and checked.
 */
val PluginProblem.isInvalidDescriptorProblem: Boolean
  get() = if (isReclassified()) {
    unwrapped is InvalidDescriptorProblem
  } else {
    this is InvalidDescriptorProblem
  }

fun PluginProblem.isInstance(pluginProblemClass: KClass<*>): Boolean =
  pluginProblemClass.isInstance(unwrapped)


object PluginProblems {
  /**
   * Resolves the problem ID to a fully qualified Kotlin class.
   *
   * The following formats are supported:
   *
   * - Fully qualified problem ID which corresponds to a class name, such as
   *    `com.jetbrains.plugin.structure.intellij.problems.ForbiddenPluginIdPrefix`
   * - Problem ID which can be resolved to a fully qualified class name in the `com.jetbrains.plugin.structure`
   *    package prefix, such as `intellij.problems.ForbiddenPluginIdPrefix`.
   */
  // TODO duplicate code
  fun resolveClass(fullyQualifiedProblemId: String): KClass<out Any>? {
    return runCatching {
      val pluginProblemJavaClass = Class.forName(fullyQualifiedProblemId, false, javaClass.getClassLoader())
      val kotlin = pluginProblemJavaClass.kotlin

      kotlin
    }.onFailure { t ->
      LOG.warn("Problem ID '$fullyQualifiedProblemId' could not be resolved to a fully qualified class corresponding to a plugin problem: {}", t.message)
    }.getOrNull()
  }
}