package com.jetbrains.plugin.structure.base.problems

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

val PluginProblem.isError: Boolean
  get() = level == PluginProblem.Level.ERROR