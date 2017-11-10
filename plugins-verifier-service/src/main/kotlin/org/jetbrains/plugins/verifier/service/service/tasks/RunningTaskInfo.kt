package org.jetbrains.plugins.verifier.service.service.tasks

import java.util.*

data class RunningTaskInfo(val taskId: ServiceTaskId,
                           val taskName: String,
                           val startedDate: Date,
                           val state: ServiceTaskStatus.State,
                           val progress: Double,
                           val totalTimeMs: Long,
                           val progressText: String)