package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

/**
 * A general plugin problem with _error_ category that is not listed as a supported error
 * in the allow-list of recognized plugin errors.
 *
 * This indicates an error on the programmer side that might unexpectingly stop the plugin validation process.
 */
class BlocklistedPluginError(val cause: PluginProblem) : PluginProblem() {
  override val level: Level
    get() = Level.ERROR
  override val message: String
    get() = "Fatal plugin problem has been detected. This problem is not registered in the list of supported of fatal plugin errors. " +
      "Please contact developers. Error: ${cause.javaClass}, message: ${cause.message}"
}