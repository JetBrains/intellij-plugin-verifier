package org.jetbrains.plugins.verifier.service.service.tasks

data class ServiceTaskStatus(val taskId: ServiceTaskId,
                             val startTime: Long,
                             var endTime: Long?,
                             var state: State,
                             val progress: ServiceTaskProgress,
                             val taskName: String) {

  fun elapsedTime(): Long = (endTime ?: System.currentTimeMillis()) - startTime

  override fun toString(): String = buildString {
    append("(")
    append("Id=" + taskId.id + "; ")
    append("State=$state; ")
    append("Time=" + (elapsedTime() / 1000) + " s; ")
    append("Progress=" + progress.getFraction() + "; ")
    append("Text=" + progress.getText() + "; ")
    append("Task-name=" + taskName)
    append(")")
  }

  enum class State {
    WAITING,
    RUNNING,
    SUCCESS,
    ERROR,
  }

}