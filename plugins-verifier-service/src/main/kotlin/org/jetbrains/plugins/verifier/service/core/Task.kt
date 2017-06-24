package org.jetbrains.plugins.verifier.service.core

import org.jetbrains.plugins.verifier.service.api.TaskId
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
abstract class Task<out R>(@Volatile var taskId: TaskId? = null) : ITask<R> {

  override fun isCancelled(): Boolean = cancelled

  override fun isSuccessful(): Boolean = result != null

  override fun isFailed(): Boolean = exception != null

  override fun result(): R = result!!

  override fun exception(): Exception = exception!!

  @Volatile
  private var cancelled: Boolean = false

  @Volatile
  private var result: R? = null

  @Volatile
  private var exception: Exception? = null

  @Volatile
  private var isStarted: Boolean = false

  abstract fun presentableName(): String

  @Throws(InterruptedException::class)
  protected abstract fun computeResult(progress: Progress): R

  fun compute(progress: Progress) {
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

  companion object {
    private val LOG = LoggerFactory.getLogger(Task::class.java)
  }

}

interface ITask<out R> {

  fun isSuccessful(): Boolean

  fun isCancelled(): Boolean

  fun isFailed(): Boolean

  fun result(): R

  fun exception(): Exception
}