package org.jetbrains.plugins.verifier.service.tasks

/**
 * Exception used to indicate that a [Task]
 * was cancelled by some [reason].
 */
class TaskCancelledException : RuntimeException {

  override val message: String

  constructor(reason: String) : super(reason) {
    this.message = reason
  }

  constructor(reason: String, cause: Throwable?) : super(reason, cause) {
    this.message = reason
  }
}