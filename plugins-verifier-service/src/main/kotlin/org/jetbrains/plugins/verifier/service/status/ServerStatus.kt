package org.jetbrains.plugins.verifier.service.status

import org.apache.commons.io.FileUtils
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.tasks.TaskId
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import org.jetbrains.plugins.verifier.service.tasks.TaskStatus
import java.util.*

data class RunningTaskInfo(val taskId: TaskId,
                           val taskName: String,
                           val startedDate: Date,
                           val state: TaskStatus.State,
                           val progress: Double,
                           val totalTimeMs: Long,
                           val progressText: String)

data class MemoryInfo(val totalMemory: Long,
                      val freeMemory: Long,
                      val usedMemory: Long,
                      val maxMemory: Long)

data class DiskUsageInfo(val totalUsage: Long)

class ServerStatus(private val taskManager: TaskManager) {

  fun getRunningTasks(): List<RunningTaskInfo> = taskManager.listTasks().map {
    RunningTaskInfo(it.taskId, it.taskName, Date(it.startTime), it.state, it.progress.getFraction(), it.elapsedTime(), it.progress.getText())
  }.sortedByDescending { it.startedDate }


  fun getDiskUsage(): DiskUsageInfo {
    val dir = FileManager.getAppHomeDirectory()
    val size = FileUtils.sizeOf(dir)
    return DiskUsageInfo(size)
  }

  fun getMemoryInfo(): MemoryInfo {
    val runtime = Runtime.getRuntime()
    return MemoryInfo(
        runtime.totalMemory(),
        runtime.freeMemory(),
        (runtime.totalMemory() - runtime.freeMemory()),
        runtime.maxMemory()
    )
  }

}