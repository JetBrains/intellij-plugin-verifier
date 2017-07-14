package org.jetbrains.plugins.verifier.service.tasks

data class TaskStatus(val taskId: TaskId,
                      val startTime: Long,
                      val endTime: Long?,
                      val state: State,
                      val progress: Double,
                      val progressText: String,
                      val presentableName: String) {
  fun elapsedTime(): Long = (endTime ?: System.currentTimeMillis()) - startTime

  override fun toString(): String {
    return "(Id=${taskId.id}; State=$state; Time=${elapsedTime() / 1000} s; Progress:$progress; Text:$progressText; Task-name: $presentableName)"
  }

  enum class State {
    WAITING,
    RUNNING,
    SUCCESS,
    ERROR,
    CANCELLED
  }

}