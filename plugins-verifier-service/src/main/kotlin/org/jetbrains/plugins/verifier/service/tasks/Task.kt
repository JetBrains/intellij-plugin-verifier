package org.jetbrains.plugins.verifier.service.tasks

/**
 * Service task to be [execute]d.
 */
abstract class Task<out T>(val presentableName: String) {

  /**
   * Executes the task and returns the result.
   *
   * [progress] can be used to track execution progress and state.
   */
  abstract fun execute(progress: ProgressIndicator): T

  final override fun toString() = presentableName

}