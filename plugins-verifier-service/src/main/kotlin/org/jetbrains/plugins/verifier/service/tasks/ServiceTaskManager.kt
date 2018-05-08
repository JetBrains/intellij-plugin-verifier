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
 * Service tasks manager responsible for enqueuing service tasks
 * and executing the completion callbacks.
 */
class ServiceTaskManager(concurrency: Int) : Closeable {
  private companion object {
    val LOG = LoggerFactory.getLogger(ServiceTaskManager::class.java)
  }

  private var nextTaskId: Long = 0

  private val executorService = Executors.newFixedThreadPool(concurrency,
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("worker-%d")
          .build()
  )

  private val _activeTasks = linkedSetOf<ServiceTaskStatus>()

  private val _finishedTasks = EvictingQueue.create<ServiceTaskStatus>(100)

  /**
   * Tasks with status either [ServiceTaskState.WAITING] or [ServiceTaskState.RUNNING]
   */
  val activeTasks: Set<ServiceTaskStatus>
    @Synchronized
    get() = _activeTasks.toSet()

  /**
   * Tasks with status either [ServiceTaskState.SUCCESS], [ServiceTaskState.ERROR] or [ServiceTaskState.CANCELLED]
   */
  val finishedTasks: Set<ServiceTaskStatus>
    @Synchronized
    get() = _finishedTasks.toSet()

  /**
   * Enqueues the [task] to be executed on a background
   * thread and returns a [descriptor] [ServiceTaskStatus] of the task
   * that can be used to monitor the task's status.
   *
   * The [onSuccess] callback with the task's result and
   * status will be called on a background thread
   * if the task is finished successfully
   *
   * The [onError] callback with the thrown exception and
   * status will be called on a background thread
   * if the task is finished abnormally.
   *
   * The [onCompletion] callback will be invoked when then task
   * completes, either successfully or abnormally.
   */
  @Synchronized
  fun <T> enqueue(task: ServiceTask<T>,
                  onSuccess: (T, ServiceTaskStatus) -> Unit,
                  onError: (Throwable, ServiceTaskStatus) -> Unit,
                  onCancelled: (Throwable, ServiceTaskStatus) -> Unit,
                  onCompletion: (ServiceTaskStatus) -> Unit): ServiceTaskStatus {
    val taskId = ++nextTaskId

    val taskProgress = DefaultProgressIndicator()
    taskProgress.fraction = 0.0
    taskProgress.text = "Waiting to start..."

    val taskStatus = ServiceTaskStatus(
        taskId,
        task.presentableName,
        taskProgress,
        Instant.now(),
        null,
        ServiceTaskState.WAITING
    )
    _activeTasks.add(taskStatus)

    val taskFuture = CompletableFuture.supplyAsync(Supplier {
      taskStatus.state = ServiceTaskState.RUNNING
      taskProgress.text = "Running..."
      task.execute(taskProgress)
    }, executorService)
    executorService

    taskFuture
        .whenComplete { result, error ->
          taskStatus.endTime = Instant.now()
          taskProgress.fraction = 1.0
          when {
            result != null -> {
              taskStatus.state = ServiceTaskState.SUCCESS
              taskProgress.text = "Finished successfully"
              onSuccess(result, taskStatus)
            }
            error.causedBy(InterruptedException::class.java) -> {
              taskStatus.state = ServiceTaskState.CANCELLED
              taskProgress.text = "Cancelled"
              onCancelled(error, taskStatus)
            }
            else -> {
              taskStatus.state = ServiceTaskState.ERROR
              taskProgress.text = "Finished with error"
              onError(error, taskStatus)
            }
          }
        }
        .whenComplete { _, _ ->
          _activeTasks.remove(taskStatus)
          _finishedTasks.add(taskStatus)
          onCompletion(taskStatus)
        }

    return taskStatus
  }

  /**
   * Enqueues the task without registering the
   * completion callbacks.
   * To specify the callbacks use [enqueue] method instead.
   */
  @Synchronized
  fun <T> enqueue(task: ServiceTask<T>) = enqueue(
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