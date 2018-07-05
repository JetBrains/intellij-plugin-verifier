package org.jetbrains.plugins.verifier.service.tasks

import java.util.concurrent.FutureTask
import java.util.concurrent.RunnableFuture

/**
 * Wrapper over [FutureTask], which was enqueued for execution in an `ExecutorService`.
 * [PriorityTask] allows to change execution order based on the task's priority:
 * 1) Tasks of the same type can be compared via implementing [Comparable] interface.
 * 2) Tasks of different types will be compared based on [taskId]
 */
class PriorityTask<V>(
    val taskId: Long,
    val task: Task<V>,
    val runnableFuture: FutureTask<V>
) : RunnableFuture<V> by runnableFuture, Comparable<PriorityTask<*>> {

  override fun compareTo(other: PriorityTask<*>): Int {
    val otherTask = other.task
    if (task.javaClass == otherTask.javaClass && task is Comparable<*>) {
      @Suppress("UNCHECKED_CAST")
      return (task as Comparable<Task<*>>).compareTo(otherTask)
    }
    return other.taskId.compareTo(taskId)
  }

}