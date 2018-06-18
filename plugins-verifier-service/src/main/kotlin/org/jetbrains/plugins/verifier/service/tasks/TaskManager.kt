package org.jetbrains.plugins.verifier.service.tasks

import java.io.Closeable

/**
 * Allows to enqueue [tasks] [Task] and execute callbacks upon completion.
 */
interface TaskManager : Closeable {
  /**
   * Tasks with state either [TaskDescriptor.State.WAITING] or [TaskDescriptor.State.RUNNING]
   */
  val activeTasks: Set<TaskDescriptor>

  /**
   * Tasks with state either [TaskDescriptor.State.SUCCESS] or [TaskDescriptor.State.ERROR]
   *
   * It contains only the last N finished tasks and can be
   * used for monitoring/debugging purposes.
   */
  val lastFinishedTasks: Set<TaskDescriptor>

  /**
   * Enqueues the [task] to be executed on a background
   * thread and returns a [descriptor] [TaskDescriptor] of the task
   * that can be used to monitor the task's state.
   *
   * The [onSuccess] callback with the task's result and
   * descriptor will be called on a background thread
   * if the task is finished successfully
   *
   * The [onError] callback with the thrown exception and
   * descriptor will be called on a background thread
   * if the task is finished abnormally.
   *
   * The [onCompletion] callback will be invoked when then task
   * completes, either successfully or abnormally.
   */
  fun <T> enqueue(
      task: Task<T>,
      onSuccess: (T, TaskDescriptor) -> Unit,
      onError: (Throwable, TaskDescriptor) -> Unit,
      onCompletion: (TaskDescriptor) -> Unit
  ): TaskDescriptor

  /**
   * Enqueues the task without registering the
   * completion callbacks.
   * To specify the callbacks use [enqueue] method instead.
   */
  fun <T> enqueue(task: Task<T>): TaskDescriptor

  /**
   * Cancels execution of the task with specified descriptor.
   *
   * If the task has not started yet, it will not.
   * If the task is running, it will be interrupted.
   */
  fun cancel(taskDescriptor: TaskDescriptor)

}