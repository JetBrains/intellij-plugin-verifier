package org.jetbrains.plugins.verifier.service.status

import com.intellij.structure.ide.IdeVersion
import org.apache.commons.io.FileUtils
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import java.text.SimpleDateFormat
import java.util.*

class ServerStatus(private val taskManager: TaskManager) {

  private val DATE_FORMAT = SimpleDateFormat("MM-dd hh:mm:ss")

  fun parameters(): List<Pair<String, *>> {
    val result = arrayListOf<Pair<String, *>>()
    result.addAll(memory())
    result.addAll(diskUsage())
    return result
  }

  fun appProperties(): List<Pair<String, String>> = Settings.values().filterNot { it.encrypted }.map { it.name to it.get() }

  fun ideFiles(): List<IdeVersion> = IdeFilesManager.ideList().sorted()

  fun getRunningTasks(): List<String> = taskManager.listTasks().sortedByDescending { it.startTime }.map {
    "${it.taskId.id}) ${it.presentableName}: Started at ${DATE_FORMAT.format(Date(it.startTime))} (${it.state} - ${it.progress * 100.0}%) (${it.elapsedTime() / 1000} seconds) ${it.progressText}"
  }

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