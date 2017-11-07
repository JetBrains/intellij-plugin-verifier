package org.jetbrains.plugins.verifier.service.tasks

import com.google.common.collect.EvictingQueue
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.jetbrains.plugins.verifier.service.progress.DefaultProgressService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier

/**
 * @author Sergey Patrikeev
 */
class ServiceTasksManager(concurrency: Int) {

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

  private val tasks = EvictingQueue.create<ServiceTaskStatus>(1000)

  @Synchronized
  fun listTasks(): List<ServiceTaskStatus> = tasks.toList()

  @Synchronized
  fun enqueue(task: ServiceTask,
              onSuccess: (ServiceTaskResult) -> Unit,
              onError: (Throwable, ServiceTaskStatus) -> Unit,
              onCompletion: (ServiceTaskStatus) -> Unit): ServiceTaskStatus {
    val taskId = ServiceTaskId(nextTaskId.incrementAndGet())

    val taskProgress = DefaultProgressService()
    taskProgress.setFraction(0.0)
    taskProgress.setText("Waiting to start...")

    val taskStatus = ServiceTaskStatus(taskId, System.currentTimeMillis(), null, ServiceTaskStatus.State.WAITING, taskProgress, task.presentableName())
    tasks.add(taskStatus)

    val taskFuture = CompletableFuture.supplyAsync(Supplier {
      taskStatus.state = ServiceTaskStatus.State.RUNNING
      taskProgress.setText("Running...")
      task.computeResult(taskProgress)
    }, executorService)

    taskFuture
        .whenComplete { result, error ->
          if (result != null) {
            taskStatus.state = ServiceTaskStatus.State.SUCCESS
            onSuccess(result)
            taskProgress.setText("Finished successfully")
          } else {
            taskStatus.state = ServiceTaskStatus.State.ERROR
            onError(error, taskStatus)
            taskProgress.setText("Finished with error")
          }
          taskStatus.endTime = System.currentTimeMillis()
          taskProgress.setFraction(1.0)
        }
        .whenComplete { _, _ ->
          onCompletion(taskStatus)
        }

    return taskStatus
  }

  fun enqueue(serviceTask: ServiceTask): ServiceTaskStatus = enqueue(serviceTask, {}, { _, _ -> }, { _ -> })

  fun stop() {
    LOG.info("Stopping task manager")
    executorService.shutdownNow()
  }

}