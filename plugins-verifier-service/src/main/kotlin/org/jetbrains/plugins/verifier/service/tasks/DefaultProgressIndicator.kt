package org.jetbrains.plugins.verifier.service.tasks

/**
 * The default implementation of the progress indicator,
 * which can be safely used in a concurrent environment.
 */
data class DefaultProgressIndicator(@Volatile override var fraction: Double = 0.0,
                                    @Volatile override var text: String = "",
                                    @Volatile override var isCancelled: Boolean = false) : ProgressIndicator {
  override fun checkCancelled() {
    if (isCancelled) {
      throw InterruptedException()
    }
  }

}