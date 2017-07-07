package org.jetbrains.plugins.verifier.service.tasks

import com.google.gson.annotations.SerializedName

data class TaskResult<out T>(@SerializedName("taskStatus") val taskStatus: TaskStatus,
                             @SerializedName("result") val result: T?,
                             @SerializedName("errorMsg") val errorMessage: String?)