package org.jetbrains.plugins.verifier.service.tasks

import com.google.common.collect.EvictingQueue
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.causedBy
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier

/**
 * Enqueues tasks and executes completion callbacks.
 */
class TaskManager(concurrency: Int) : Closeable {
  private companion object {
    val LOG = LoggerFactory.getLogger(TaskManager::class.java)
  }

  private var nextTaskId: Long = 0

  private val executorService = Executors.newFixedThreadPool(concurrency,
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("worker-%d")
          .build()
  )

  private val _activeTasks = linkedSetOf<TaskDescriptor>()

  private val _finishedTasks = EvictingQueue.create<TaskDescriptor>(100)

  /**
   * Tasks with state either [TaskDescriptor.State.WAITING] or [TaskDescriptor.State.RUNNING]
   */
  val activeTasks: Set<TaskDescriptor>
    @Synchronized
    get() = _activeTasks.toSet()

  /**
   * Tasks with state either [TaskDescriptor.State.SUCCESS], [TaskDescriptor.State.ERROR] or [TaskDescriptor.State.CANCELLED]
   */
  val finishedTasks: Set<TaskDescriptor>
    @Synchronized
    get() = _finishedTasks.toSet()

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
  @Synchronized
  fun <T> enqueue(task: Task<T>,
                  onSuccess: (T, TaskDescriptor) -> Unit,
                  onError: (Throwable, TaskDescriptor) -> Unit,
                  onCancelled: (Throwable, TaskDescriptor) -> Unit,
                  onCompletion: (TaskDescriptor) -> Unit): TaskDescriptor {
    val taskId = ++nextTaskId

    val taskProgress = DefaultProgressIndicator()
    taskProgress.fraction = 0.0
    taskProgress.text = "Waiting to start..."

    val descriptor = TaskDescriptor(
        taskId,
        task.presentableName,
        taskProgress,
        Instant.now(),
        null,
        TaskDescriptor.State.WAITING
    )
    _activeTasks.add(descriptor)

    val taskFuture = CompletableFuture.supplyAsync(Supplier {
      descriptor.state = TaskDescriptor.State.RUNNING
      taskProgress.text = "Running..."
      task.execute(taskProgress)
    }, executorService)
    executorService

    taskFuture
        .whenComplete { result, error ->
          descriptor.endTime = Instant.now()
          taskProgress.fraction = 1.0
          when {
            result != null -> {
              descriptor.state = TaskDescriptor.State.SUCCESS
              taskProgress.text = "Finished successfully"
              onSuccess(result, descriptor)
            }
            error.causedBy(InterruptedException::class.java) -> {
              descriptor.state = TaskDescriptor.State.CANCELLED
              taskProgress.text = "Cancelled"
              onCancelled(error, descriptor)
            }
            else -> {
              descriptor.state = TaskDescriptor.State.ERROR
              taskProgress.text = "Finished with error"
              onError(error, descriptor)
            }
          }
        }
        .whenComplete { _, _ ->
          _activeTasks.remove(descriptor)
          _finishedTasks.add(descriptor)
          onCompletion(descriptor)
        }

    return descriptor
  }

  /**
   * Enqueues the task without registering the
   * completion callbacks.
   * To specify the callbacks use [enqueue] method instead.
   */
  @Synchronized
  fun <T> enqueue(task: Task<T>) = enqueue(
      task,
      { _, _ -> },
      { _, _ -> },
      { _, _ -> },
      { _ -> }
  )

  @Synchronized
  override fun close() {
    LOG.info("Stopping task manager")
    executorService.shutdownNow()
  }

}