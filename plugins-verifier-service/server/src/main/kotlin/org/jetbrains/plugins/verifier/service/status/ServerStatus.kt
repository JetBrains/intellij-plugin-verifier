package org.jetbrains.plugins.verifier.service.status

import com.intellij.structure.domain.IdeVersion
import org.apache.commons.io.FileUtils
import org.jetbrains.plugins.verifier.service.core.TaskManager
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager

object ServerStatus {
  fun parameters(): List<Pair<String, *>> {
    val result = arrayListOf<Pair<String, *>>()
    result.addAll(memory())
    result.addAll(diskUsage())
    return result
  }

  fun appProperties(): List<Pair<String, String>> = Settings.values().map { it.name to it.get() }

  fun ideFiles(): List<IdeVersion> = IdeFilesManager.ideList().sorted()

  fun getRunningTasks(): List<String> = TaskManager.listTasks().map {
    "${it.taskId.id}) ${it.presentableName} (${it.state} - ${it.progress * 100.0}%) (${it.elapsedTime() / 1000} seconds) ${it.progressText}"
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