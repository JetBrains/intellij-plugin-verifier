package com.jetbrains.plugin.structure.fleet

import com.jetbrains.plugin.structure.base.problems.*
import java.nio.file.Files
import java.nio.file.Path

private const val MAX_FILE_SIZE = 100L * 1024 * 1024 //100MB
private const val MAX_PLUGIN_SIZE = 400L * 1024 * 1024 //400MB
private const val MAX_FILES = 1000L

const val SHIP_PLUGIN_ID = "SHIP"

internal class FileChecker(val pluginId: String?) {
  private var sumSize = 0L
  private var sumFiles = 0

  val problems = mutableListOf<PluginProblem>()

  fun addFile(file: Path): Boolean {
    if (sumFiles <= MAX_FILES) {
      sumFiles += 1
      if (sumFiles > MAX_FILES) {
        problems.add(TooManyFiles(MAX_FILES))
      }
    }

    if (!Files.exists(file)) {
      problems.add(MissedFile(file.fileName.toString()))
      return false
    }
    val size = Files.size(file)
    if (size > MAX_FILE_SIZE) {
      problems.add(FileTooBig(file.toString(), MAX_FILE_SIZE))
    }
    if (sumSize <= MAX_PLUGIN_SIZE) {
      sumSize += size
      if (sumSize > MAX_PLUGIN_SIZE) {
        problems.add(PluginFileSizeIsTooLarge(MAX_PLUGIN_SIZE))
      }
    }
    return problems.isEmpty()
  }
}