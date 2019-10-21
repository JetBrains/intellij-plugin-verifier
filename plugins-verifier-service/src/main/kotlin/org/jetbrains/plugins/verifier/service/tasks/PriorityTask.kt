package org.jetbrains.plugins.verifier.service.tasks

import java.util.concurrent.FutureTask
import java.util.concurrent.RunnableFuture

/**
 * Wrapper over task enqueued for execution in `ExecutorService`
 * that allows to change execution order based on the task's priority.
 *
 * [Task]s that implement [Comparable] will be executed in order of comparison.
 * Other tasks will be executed in order of [TaskDescriptor.taskId].
 *
 * Withing one `ExecutorService` all [task]s must be of the same class.
 */
internal class PriorityTask<V>(
  val taskDescriptor: TaskDescriptor,
  val task: Task<V>,
  val runnableFuture: FutureTask<V>
) : RunnableFuture<V> by runnableFuture, Comparable<PriorityTask<*>> {

  //Used by `PriorityBlockingQueue` supplied to `ExecutorService`.
  override fun compareTo(other: PriorityTask<*>): Int {
    val otherTask = other.task
    /**
     * Assert that we don't try to compare tasks of different types.
     */
    require(task.javaClass == other.task.javaClass)
    if (task is Comparable<*>) {
      @Suppress("UNCHECKED_CAST")
      return (task as Comparable<Task<*>>).compareTo(otherTask)
    }
    return taskDescriptor.taskId.compareTo(other.taskDescriptor.taskId)
  }

}