package org.jetbrains.plugins.verifier.service.core

import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
abstract class Task<R>() {

  @Volatile
  var result: R? = null
    private set
    get() {
      if (exception != null) {
        throw exception as Exception
      }
      return field
    }

  var exception: Exception? = null

  abstract fun presentableName(): String

  @Throws(InterruptedException::class)
  abstract fun computeImpl(progress: Progress): R

  fun compute(progress: Progress) {
    try {
      result = computeImpl(progress)
    } catch(e: InterruptedException) {
      LOG.info("The task $this was cancelled")
      exception = CancelledException()
    } catch(e: Exception) {
      LOG.error("An exception in the Task $this", e)
      exception = e
    }
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(Task::class.java)
  }

  class CancelledException() : RuntimeException("The task was cancelled", null, false, false)

}