package org.jetbrains.plugins.verifier.service.tasks

import org.jetbrains.plugins.verifier.service.progress.DefaultProgress
import org.jetbrains.plugins.verifier.service.progress.TaskProgress

class Worker<T>(val task: Task<T>,
                val taskId: TaskId,
                val onSuccess: (TaskResult<T>) -> Unit,
                val onError: (t: Throwable, taskStatus: TaskStatus, task: Task<T>) -> Unit,
                val onCompletion: (taskStatus: TaskStatus, task: Task<T>) -> Unit,
                val taskManager: TaskManager,
                @Volatile var result: T? = null,
                @Volatile var errorMsg: String? = null,
                @Volatile var state: TaskStatus.State = TaskStatus.State.WAITING,
                val startTime: Long = System.currentTimeMillis(),
                @Volatile var endTime: Long? = null,
                val progress: TaskProgress = DefaultProgress()) : Runnable {

  override fun run() {
    TaskManager.LOG.info("Task #$taskId ${task.presentableName()} is starting")
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
      TaskManager.LOG.error("Unable to compute task #$taskId", e)
    } catch (e: Error) {
      TaskManager.LOG.error("Fatal error during computation of a task #$taskId", e)
      throw e
    } finally {
      endTime = System.currentTimeMillis()
      taskManager.onComplete(taskId)
      TaskManager.LOG.info("Task #$taskId is completed with $state")

      //execute callbacks
      val status = TaskStatus(taskId, startTime, endTime, state, progress.getProgress(), progress.getText(), task.presentableName())
      try {
        if (task.isSuccessful()) {
          onSuccess(TaskResult(status, task.result(), null))
        } else if (task.isFailed()) {
          onError(task.exception(), status, task)
        }
      } finally {
        onCompletion(status, task)
      }
    }

  }

}