package org.jetbrains.plugins.verifier.service.server.status

import org.apache.commons.io.FileUtils
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskId
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskStatus
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTasksManager
import org.jetbrains.plugins.verifier.service.storage.FileManager
import java.util.*

data class RunningTaskInfo(val taskId: ServiceTaskId,
                           val taskName: String,
                           val startedDate: Date,
                           val state: ServiceTaskStatus.State,
                           val progress: Double,
                           val totalTimeMs: Long,
                           val progressText: String)

data class MemoryInfo(val totalMemory: Long,
                      val freeMemory: Long,
                      val usedMemory: Long,
                      val maxMemory: Long)

data class DiskUsageInfo(val totalUsage: Long)

class ServerStatus(private val taskManager: ServiceTasksManager) {

  fun getRunningTasks(): List<RunningTaskInfo> = taskManager.listTasks()
      .map { it.getRunningTaskInfo() }
      .sortedByDescending { it.startedDate }

  private fun ServiceTaskStatus.getRunningTaskInfo() =
      RunningTaskInfo(taskId, taskName, Date(startTime), state, progress.getFraction(), elapsedTime(), progress.getText())


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