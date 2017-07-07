package org.jetbrains.plugins.verifier.service.tasks

import com.google.gson.annotations.SerializedName

data class TaskStatus(@SerializedName("taskId") val taskId: TaskId,
                      @SerializedName("startTime") val startTime: Long,
                      @SerializedName("endTime") val endTime: Long?,
                      @SerializedName("status") val state: State,
                      @SerializedName("progress") val progress: Double,
                      @SerializedName("progressText") val progressText: String,
                      @SerializedName("taskName") val presentableName: String) {
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