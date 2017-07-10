package org.jetbrains.plugins.verifier.service.status

import org.apache.commons.io.FileUtils
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.tasks.TaskId
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import org.jetbrains.plugins.verifier.service.tasks.TaskStatus
import java.util.*

class ServerStatus(private val taskManager: TaskManager) {

  fun health(): List<Pair<String, *>> {
    val result = arrayListOf<Pair<String, *>>()
    result.addAll(memory())
    result.addAll(diskUsage())
    return result
  }

  data class RunningTask(val taskId: TaskId,
                         val taskName: String,
                         val startedDate: Date,
                         val state: TaskStatus.State,
                         val progress: Double,
                         val totalTimeMs: Long,
                         val progressText: String)

  fun runningTasks(): List<RunningTask> = taskManager.listTasks().map {
    RunningTask(it.taskId, it.presentableName, Date(it.startTime), it.state, it.progress, it.elapsedTime(), it.progressText)
  }.sortedByDescending { it.startedDate }


  private fun diskUsage(): List<Pair<String, *>> {
    val dir = FileManager.getAppHomeDirectory()
    val size = FileUtils.sizeOf(dir)
    return listOf(
        "Total disk usage: " to "%.3f GB".format(size.toDouble() / FileUtils.ONE_GB)
    )
  }

  private fun memory(): List<Pair<String, Long>> {
    val runtime = Runtime.getRuntime()
    val mb = 1024 * 1024

    return arrayListOf(
        "Total memory: " to runtime.totalMemory() / mb,
        "Free memory: " to runtime.freeMemory() / mb,
        "Used memory: " to (runtime.totalMemory() - runtime.freeMemory()) / mb,
        "Max memory: " to runtime.maxMemory() / mb
    )
  }

}