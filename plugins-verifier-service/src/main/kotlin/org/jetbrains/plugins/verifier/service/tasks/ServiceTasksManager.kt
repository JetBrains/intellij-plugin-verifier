package org.jetbrains.plugins.verifier.service.tasks

import com.google.common.collect.EvictingQueue
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier

/**
 * Service tasks manager responsible for enqueuing service [tasks] [ServiceTask]
 * and executing the completion callbacks.
 */
class ServiceTasksManager(concurrency: Int, maxKeepResults: Int) : Closeable {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(ServiceTasksManager::class.java)
  }

  private val nextTaskId = AtomicLong()

  private val executorService = Executors.newFixedThreadPool(concurrency,
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("worker-%d")
          .build()
  )

  private val tasks = EvictingQueue.create<ServiceTaskStatus>(maxKeepResults)

  @Synchronized
  fun getRunningTasks(): List<ServiceTaskStatus> = tasks
      .sortedByDescending { it.startTime }

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
                  onCompletion: (ServiceTaskStatus) -> Unit): ServiceTaskStatus {
    val taskId = nextTaskId.incrementAndGet()

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
    tasks.add(taskStatus)

    val taskFuture = CompletableFuture.supplyAsync(Supplier {
      taskStatus.state = ServiceTaskState.RUNNING
      taskProgress.text = "Running..."
      task.execute(taskProgress)
    }, executorService)

    taskFuture
        .whenComplete { result, error ->
          if (result != null) {
            taskStatus.state = ServiceTaskState.SUCCESS
            onSuccess(result, taskStatus)
            taskProgress.text = "Finished successfully"
          } else {
            taskStatus.state = ServiceTaskState.ERROR
            onError(error, taskStatus)
            taskProgress.text = "Finished with error"
          }
          taskStatus.endTime = Instant.now()
          taskProgress.fraction = 1.0
        }
        .whenComplete { _, _ ->
          onCompletion(taskStatus)
        }

    return taskStatus
  }

  /**
   * Enqueues the task without registering the
   * completion callbacks.
   * To specify the callbacks use [enqueue] method instead.
   */
  fun <T> enqueue(task: ServiceTask<T>) = enqueue(
      task,
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