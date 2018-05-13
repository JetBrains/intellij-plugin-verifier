package com.jetbrains.pluginverifier.misc

import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

fun ExecutorService.shutdownAndAwaitTermination(timeout: Long, timeUnit: TimeUnit) {
  // Disable new tasks from being submitted
  shutdown()
  try {
    // Wait a while for existing tasks to terminate
    if (!awaitTermination(timeout, timeUnit)) {
      // Cancel currently executing tasks
      shutdownNow()
      // Wait a while for tasks to respond to being cancelled
      if (!awaitTermination(timeout, timeUnit)) {
        throw RuntimeException("Executor didn't terminate")
      }
    }
  } catch (ie: InterruptedException) {
    // (Re-) Cancel if current thread also interrupted
    shutdownNow()
    // Preserve interrupt status
    Thread.currentThread().interrupt()
  }
}