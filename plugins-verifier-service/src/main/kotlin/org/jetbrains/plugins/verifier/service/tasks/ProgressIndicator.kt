package org.jetbrains.plugins.verifier.service.tasks

/**
 * The progress indicator is used to monitor
 * and control the [task] [ServiceTask] execution progress,
 * invoke and check the cancellation.
 */
interface ProgressIndicator {

  var fraction: Double

  var text: String

  var isCancelled: Boolean

  @Throws(InterruptedException::class)
  fun checkCancelled()

}