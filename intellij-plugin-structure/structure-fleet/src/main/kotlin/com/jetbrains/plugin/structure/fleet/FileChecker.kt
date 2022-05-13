package com.jetbrains.plugin.structure.fleet

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import java.nio.file.Files
import java.nio.file.Path

private const val MAX_FILE_SIZE = 100L * 1024 * 1024 //100MB
private const val MAX_PLUGIN_SIZE = 400L * 1024 * 1024 //400MB
private const val MAX_FILES = 1000

internal class FileChecker(val pluginId: String?) {
  private var sumSize = 0L
  private var sumFiles = 0

  val problems = mutableListOf<PluginProblem>()

  fun addFile(file: Path): Boolean {
    if (sumFiles <= MAX_FILES) {
      sumFiles += 1
      if (sumFiles > MAX_FILES) {
        problems.add(TooManyFiles(pluginId))
      }
    }

    if (!Files.exists(file)) {
      problems.add(MissedFile(file.fileName.toString()))
      return false
    }
    val size = Files.size(file)
    if (size > MAX_FILE_SIZE) {
      problems.add(FileTooBig(file.toString()))
    }
    if (sumSize <= MAX_PLUGIN_SIZE) {
      sumSize += size
      if (sumSize > MAX_PLUGIN_SIZE) {
        problems.add(PluginTooBig(pluginId))
      }
    }
    return problems.isEmpty()
  }
}

private class PluginTooBig(val pluginId: String?) : PluginProblem() {
  override val message: String
    get() = "Plugin $pluginId is bigger than max allowed size: $MAX_PLUGIN_SIZE"

  override val level = Level.ERROR
}

private class TooManyFiles(val pluginId: String?) : PluginProblem() {
  override val message: String
    get() = "Plugin $pluginId has more files than allowed: $MAX_PLUGIN_SIZE"

  override val level = Level.ERROR
}

private class FileTooBig(val file: String) : PluginProblem() {
  override val message: String
    get() = "File $file is bigger than max allowed size: $MAX_FILE_SIZE"

  override val level = Level.ERROR
}

private class MissedFile(val file: String) : PluginProblem() {
  override val message: String
    get() = "File $file is missed"

  override val level = Level.ERROR
}
