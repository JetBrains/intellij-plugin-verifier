package org.jetbrains.plugins.verifier.service.service.tasks

data class ServiceTaskStatus(val taskId: ServiceTaskId,
                             val startTime: Long,
                             var endTime: Long?,
                             var state: State,
                             val progress: ServiceTaskProgress,
                             val taskName: String) {

  fun elapsedTime(): Long = (endTime ?: System.currentTimeMillis()) - startTime

  override fun toString(): String = "(Id=${taskId.id}; State=$state; Time=${elapsedTime() / 1000} s; Progress:${progress.getFraction()}; Text:${progress.getText()}; Task-name: $taskName)"

  enum class State {
    WAITING,
    RUNNING,
    SUCCESS,
    ERROR,
  }

}