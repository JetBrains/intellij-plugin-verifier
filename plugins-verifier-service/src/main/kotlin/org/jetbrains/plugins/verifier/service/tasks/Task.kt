package org.jetbrains.plugins.verifier.service.tasks

import org.jetbrains.plugins.verifier.service.progress.TaskProgress
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
abstract class Task<out R>(@Volatile var taskId: TaskId? = null) {

  companion object {
    private val LOG = LoggerFactory.getLogger(Task::class.java)
  }

  fun isCancelled(): Boolean = cancelled

  fun isSuccessful(): Boolean = result != null

  fun isFailed(): Boolean = exception != null

  fun result(): R = result!!

  fun exception(): Exception = exception!!

  @Volatile
  private var cancelled: Boolean = false

  @Volatile
  private var result: R? = null

  @Volatile
  private var exception: Exception? = null

  @Volatile
  private var isStarted: Boolean = false

  abstract fun presentableName(): String

  protected abstract fun computeResult(progress: TaskProgress): R

  fun compute(progress: TaskProgress) {
    check(!isStarted, { "The task #$taskId must not be started twice" })
    isStarted = true
    try {
      result = computeResult(progress)
    } catch(e: InterruptedException) {
      LOG.info("The task #$taskId was cancelled")
      cancelled = true
    } catch(e: Exception) {
      LOG.error("Exception in the task #$taskId", e)
      exception = e
    }
  }

}