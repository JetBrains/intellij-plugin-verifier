package org.jetbrains.plugins.verifier.service.tasks

data class TaskResult<out T>(val taskStatus: TaskStatus,
                             val result: T?,
                             val errorMessage: String?)