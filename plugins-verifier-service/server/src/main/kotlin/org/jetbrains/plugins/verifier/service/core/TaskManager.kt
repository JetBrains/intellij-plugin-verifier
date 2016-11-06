package org.jetbrains.plugins.verifier.service.core

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.jetbrains.plugins.verifier.service.api.Result
import org.jetbrains.plugins.verifier.service.api.TaskId
import org.jetbrains.plugins.verifier.service.api.TaskStatus
import org.jetbrains.plugins.verifier.service.api.TaskStatus.State.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * @author Sergey Patrikeev
 */
object TaskManager : ITaskManager {

  const val MAX_RUNNING_TASKS = 100

  private val PARALLEL_LIMIT: Int by lazy {
    Math.max(8, Runtime.getRuntime().availableProcessors())
  }

  private var counter: Int = 0

  private val service = Executors.newFixedThreadPool(PARALLEL_LIMIT, ThreadFactoryBuilder().setDaemon(true).setNameFormat("worker-%d").build())

  private val tasks: MutableMap<TaskId, Pair<Future<*>, Worker<*>>> = hashMapOf()

  private val PRESERVE_RESULTS_NUMBER: Int = 100

  private val MINIMUM_TIME_WAITING_GET_MILLIS: Long = 10 * 1000

  private val completedTasks: Queue<TaskId> = LinkedList()

  private val LOG: Logger = LoggerFactory.getLogger(TaskManager::class.java)

  @Synchronized
  override fun runningTasksNumber(): Int = tasks.values.count { it.second.state == WAITING || it.second.state == TaskStatus.State.RUNNING }

  @Synchronized
  override fun get(taskId: TaskId): Result<*>? {
    val worker = (tasks[taskId] ?: return null).second
    val taskStatus = TaskStatus(taskId, worker.startTime, worker.endTime, worker.state,
        worker.progress.getProgress(), worker.progress.getText(), worker.task.presentableName())

    return when (worker.state) {
      WAITING, CANCELLED, RUNNING -> Result(taskStatus, null, null)
      SUCCESS -> Result(taskStatus, worker.result!!, null)
      ERROR -> Result(taskStatus, null, worker.errorMsg!!)
    }
  }

  @Synchronized
  override fun listTasks(): List<TaskStatus> = tasks.entries.map {
    val worker = it.value.second
    TaskStatus(it.key, worker.startTime, worker.endTime, worker.state, worker.progress.getProgress(), worker.progress.getText(), worker.task.presentableName())
  }

  @Synchronized
  override fun <T> enqueue(task: Task<T>,
                           onSuccess: (Result<T>) -> Unit,
                           onError: (Throwable, TaskStatus, Task<T>) -> Unit,
                           onCompletion: (TaskStatus, Task<T>) -> Unit): TaskId {
    val taskId = TaskId(counter++)

    task.taskId = taskId

    val worker = Worker(task, taskId, onSuccess, onError, onCompletion)

    val future = service.submit(worker)

    tasks.put(taskId, future to worker)

    return taskId
  }

  @Synchronized
  override fun <T> enqueue(task: Task<T>): TaskId = enqueue(task, {}, { t, tst, task -> }, { tst, t -> })

  @Synchronized
  override fun cancel(taskId: TaskId): Boolean {
    tasks[taskId]?.first?.cancel(true)
    val removed = tasks.remove(taskId)
    return removed != null
  }

  @Synchronized
  private fun onComplete(taskId: TaskId) {
    completedTasks.add(taskId)
    if (completedTasks.size < PRESERVE_RESULTS_NUMBER) {
      return
    }
    val oldestId = completedTasks.peek()
    val oldestTask = tasks[oldestId]!!
    val oldestResultTime = System.currentTimeMillis() - oldestTask.second.endTime!!
    if (oldestResultTime < MINIMUM_TIME_WAITING_GET_MILLIS) {
      return
    }
    tasks.remove(oldestId)
    completedTasks.remove()
  }

  private class Worker<T>(val task: Task<T>,
                          val taskId: TaskId,
                          val onSuccess: (Result<T>) -> Unit,
                          val onError: (t: Throwable, taskStatus: TaskStatus, task: Task<T>) -> Unit,
                          val onCompletion: (taskStatus: TaskStatus, task: Task<T>) -> Unit,
                          @Volatile var result: T? = null,
                          @Volatile var errorMsg: String? = null,
                          @Volatile var state: TaskStatus.State = WAITING,
                          val startTime: Long = System.currentTimeMillis(),
                          @Volatile var endTime: Long? = null,
                          val progress: Progress = DefaultProgress()) : Runnable {

    override fun run() {
      LOG.info("Task #$taskId ${task.presentableName()} is starting")
      state = TaskStatus.State.RUNNING
      try {
        task.compute(progress)
        if (task.isSuccessful()) {
          state = TaskStatus.State.SUCCESS
          result = task.result()
        } else if (task.isCancelled()) {
          state = TaskStatus.State.CANCELLED
        } else {
          state = TaskStatus.State.ERROR
          errorMsg = "The task #$taskId failed due to ${task.exception().message ?: task.exception().javaClass.name}"
        }
      } catch (e: Exception) {
        LOG.error("Unable to compute task #$taskId", e)
      } finally {
        endTime = System.currentTimeMillis()
        onComplete(taskId)
        LOG.info("Task #$taskId is completed with $state")
      }

      //execute callbacks
      val status = TaskStatus(taskId, startTime, endTime, state, progress.getProgress(), progress.getText(), task.presentableName())
      try {
        if (task.isSuccessful()) {
          onSuccess(Result(status, task.result(), null))
        } else if (task.isFailed()) {
          onError(task.exception(), status, task)
        }
      } finally {
        onCompletion(status, task)
      }
    }

  }

}

interface ITaskManager {

  fun <T> enqueue(task: Task<T>): TaskId

  fun <T> enqueue(task: Task<T>, onSuccess: (Result<T>) -> Unit, onError: (Throwable, TaskStatus, Task<T>) -> Unit, onCompletion: (TaskStatus, Task<T>) -> Unit): TaskId

  fun cancel(taskId: TaskId): Boolean

  fun get(taskId: TaskId): Result<*>?

  fun listTasks(): List<TaskStatus>

  fun runningTasksNumber(): Int
}
