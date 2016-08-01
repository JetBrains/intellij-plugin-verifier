package org.jetbrains.plugins.verifier.service.api

/**
 * @author Sergey Patrikeev
 */
data class TaskId(val id: Int)

enum class Status {
  WAITING,
  RUNNING,
  COMPLETE
}

data class TaskStatusDescriptor(val taskId: TaskId,
                                val startTime: Long,
                                val endTime: Long?,
                                val status: Status,
                                val progress: Double,
                                val progressText: String,
                                val presentableName: String) {
  fun completionTime(): Long = (endTime ?: System.currentTimeMillis()) - startTime

  override fun toString(): String {
    return "(Id=${taskId.id}; Status=$status; Time=${completionTime() / 1000} s; Progress:$progress; Text:$progressText; Task-name: $presentableName)"
  }
}

data class Result<out T>(val taskStatus: TaskStatusDescriptor, val result: T? = null)