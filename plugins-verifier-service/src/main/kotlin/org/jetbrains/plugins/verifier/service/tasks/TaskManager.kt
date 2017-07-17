package org.jetbrains.plugins.verifier.service.tasks

import com.google.common.collect.EvictingQueue
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.jetbrains.plugins.verifier.service.progress.DefaultProgress
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

/**
 * @author Sergey Patrikeev
 */
class TaskManager(concurrency: Int) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(TaskManager::class.java)
  }

  private val nextTaskId = AtomicInteger()

  private val executorService = Executors.newFixedThreadPool(concurrency,
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("worker-%d")
          .build()
  )

  private val tasks = EvictingQueue.create<TaskStatus>(1000)

  @Synchronized
  fun listTasks(): List<TaskStatus> = tasks.toList()

  @Synchronized
  fun <Res, Tsk : Task<Res>> enqueue(task: Tsk,
                                     onSuccess: (Res) -> Unit,
                                     onError: (Throwable, TaskStatus, Tsk) -> Unit,
                                     onCompletion: (TaskStatus, Tsk) -> Unit): TaskStatus {
    val taskId = TaskId(nextTaskId.incrementAndGet())

    val taskProgress = DefaultProgress()
    taskProgress.setFraction(0.0)
    taskProgress.setText("Waiting to start...")

    val taskStatus = TaskStatus(taskId, System.currentTimeMillis(), null, TaskStatus.State.WAITING, taskProgress, task.presentableName())
    tasks.add(taskStatus)

    val taskFuture = CompletableFuture.supplyAsync(Supplier {
      taskStatus.state = TaskStatus.State.RUNNING
      task.computeResult(taskProgress)
    }, executorService)

    taskFuture
        .whenComplete { result, error ->
          if (result != null) {
            taskStatus.state = TaskStatus.State.SUCCESS
            onSuccess(result)
          } else {
            taskStatus.state = TaskStatus.State.ERROR
            onError(error, taskStatus, task)
          }
          taskStatus.endTime = System.currentTimeMillis()
        }
        .whenComplete { _, _ ->
          onCompletion(taskStatus, task)
        }

    return taskStatus
  }

  fun <T> enqueue(task: Task<T>): TaskStatus = enqueue(task, {}, { _, _, _ -> }, { _, _ -> })

  fun stop() {
    LOG.info("Stopping task manager")
    executorService.shutdownNow()
  }

}