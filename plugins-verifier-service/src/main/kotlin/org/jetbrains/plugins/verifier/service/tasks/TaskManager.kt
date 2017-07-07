package org.jetbrains.plugins.verifier.service.tasks

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.jetbrains.plugins.verifier.service.tasks.TaskStatus.State.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * @author Sergey Patrikeev
 */
class TaskManager(private val maxRunningTasks: Int) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(TaskManager::class.java)
  }

  private val PARALLEL_LIMIT: Int by lazy {
    Math.max(8, Runtime.getRuntime().availableProcessors())
  }

  private var counter: Int = 0

  private val service = Executors.newFixedThreadPool(PARALLEL_LIMIT, ThreadFactoryBuilder().setDaemon(true).setNameFormat("worker-%d").build())

  private val tasks: MutableMap<TaskId, Pair<Future<*>, Worker<*>>> = hashMapOf()

  private val PRESERVE_RESULTS_NUMBER: Int = 100

  private val MINIMUM_TIME_WAITING_GET_MILLIS: Long = 10 * 1000

  private val completedTasks: Queue<TaskId> = LinkedList()

  fun isBusy(): Boolean = runningTasksNumber() > maxRunningTasks

  @Synchronized
  fun runningTasksNumber(): Int = tasks.values.count { it.second.state == WAITING || it.second.state == TaskStatus.State.RUNNING }

  @Synchronized
  fun get(taskId: TaskId): TaskResult<*>? {
    val worker = (tasks[taskId] ?: return null).second
    val taskStatus = TaskStatus(taskId, worker.startTime, worker.endTime, worker.state,
        worker.progress.getProgress(), worker.progress.getText(), worker.task.presentableName())

    return when (worker.state) {
      WAITING, CANCELLED, RUNNING -> TaskResult(taskStatus, null, null)
      SUCCESS -> TaskResult(taskStatus, worker.result!!, null)
      ERROR -> TaskResult(taskStatus, null, worker.errorMsg!!)
    }
  }

  @Synchronized
  fun listTasks(): List<TaskStatus> = tasks.entries.map {
    val worker = it.value.second
    TaskStatus(it.key, worker.startTime, worker.endTime, worker.state, worker.progress.getProgress(), worker.progress.getText(), worker.task.presentableName())
  }

  @Synchronized
  fun <T> enqueue(task: Task<T>,
                  onSuccess: (TaskResult<T>) -> Unit,
                  onError: (Throwable, TaskStatus, Task<T>) -> Unit,
                  onCompletion: (TaskStatus, Task<T>) -> Unit): TaskId {
    val taskId = TaskId(counter++)

    task.taskId = taskId

    val worker = Worker(task, taskId, onSuccess, onError, onCompletion, this)

    val future = service.submit(worker)

    tasks.put(taskId, future to worker)

    return taskId
  }

  @Synchronized
  fun <T> enqueue(task: Task<T>): TaskId = enqueue(task, {}, { _, _, _ -> }, { _, _ -> })

  @Synchronized
  fun cancel(taskId: TaskId): Boolean {
    tasks[taskId]?.first?.cancel(true)
    val removed = tasks.remove(taskId)
    return removed != null
  }

  @Synchronized
  internal fun onComplete(taskId: TaskId) {
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

}