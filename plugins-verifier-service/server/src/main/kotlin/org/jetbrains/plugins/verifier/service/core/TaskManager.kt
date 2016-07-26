package org.jetbrains.plugins.verifier.service.core

import org.jetbrains.plugins.verifier.service.api.Result
import org.jetbrains.plugins.verifier.service.api.Status
import org.jetbrains.plugins.verifier.service.api.TaskId
import org.jetbrains.plugins.verifier.service.api.TaskStatusDescriptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * @author Sergey Patrikeev
 */
object TaskManager : ITaskManager {
  private const val MAX_EXECUTORS = 8

  private var counter: Int = 0

  private val service = Executors.newFixedThreadPool(MAX_EXECUTORS)

  private val tasks: MutableMap<TaskId, Pair<Future<*>, Worker>> = hashMapOf()

  private val PRESERVE_RESULTS_NUMBER: Int = 100
  private val MINIMUM_TIME_WAITING_GET_MILLIS: Long = 10 * 1000

  private val completedTasks: Queue<TaskId> = LinkedList()

  private val LOG: Logger = LoggerFactory.getLogger(TaskManager::class.java)

  @Synchronized
  override fun get(taskId: TaskId): Result<*>? {
    val worker = (tasks[taskId] ?: return null).second
    val result = worker.task.result
    return Result(TaskStatusDescriptor(taskId, worker.startTime, worker.endTime, worker.status, worker.progress.getProgress(), worker.progress.getText(), worker.task.presentableName()), result)
  }

  @Synchronized
  override fun listTasks(): List<TaskStatusDescriptor> = tasks.entries.map {
    val worker = it.value.second
    TaskStatusDescriptor(it.key, worker.startTime, worker.endTime, worker.status, worker.progress.getProgress(), worker.progress.getText(), worker.task.presentableName())
  }

  @Synchronized
  override fun <T> enqueue(task: Task<T>): TaskId {
    val taskId = TaskId(counter++)

    task.taskId = taskId

    val worker = Worker(task, taskId)

    val future = service.submit(worker)

    tasks.put(taskId, future to worker)

    return taskId
  }

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

  private class Worker(val task: Task<*>,
                       val taskId: TaskId,
                       @Volatile var status: Status = Status.WAITING,
                       val startTime: Long = System.currentTimeMillis(),
                       @Volatile var endTime: Long? = null,
                       val progress: Progress = DefaultProgress()) : Runnable {

    override fun run() {
      LOG.info("Task #$taskId is starting")
      status = Status.RUNNING
      try {
        task.compute(progress)
      } finally {
        status = Status.COMPLETE
        endTime = System.currentTimeMillis()
        onComplete(taskId)
        LOG.info("Task #$taskId is finished")
      }
    }

  }

}

interface ITaskManager {

  fun <T> enqueue(task: Task<T>): TaskId

  fun cancel(taskId: TaskId): Boolean

  fun get(taskId: TaskId): Result<*>?

  fun listTasks(): List<TaskStatusDescriptor>
}
