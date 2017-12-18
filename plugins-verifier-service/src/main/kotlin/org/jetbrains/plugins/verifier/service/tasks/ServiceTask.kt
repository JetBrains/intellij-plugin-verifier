package org.jetbrains.plugins.verifier.service.tasks

/**
 * Service task is a piece of work to be executed.
 *
 * The [presentableName] is used to display the task
 * and its [progress] [ProgressIndicator] on the
 * server [status] [org.jetbrains.plugins.verifier.service.server.servlets.InfoServlet] page .
 */
abstract class ServiceTask<out T>(val presentableName: String) {

  /**
   * Executes the task and returns the result.
   */
  abstract fun execute(progress: ProgressIndicator): T

  override fun toString(): String = presentableName

}