package com.jetbrains.plugin.structure.base.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import java.io.File

open class IncorrectPluginFile(file: File, expectedFileType: String) : PluginProblem() {
  override val level: Level = Level.ERROR
  override val message: String = "Incorrect plugin file ${file.name}. Must be a $expectedFileType"
}

data class UnableToExtractZip(val pluginFile: File) : PluginProblem() {
  override val level: Level = Level.ERROR
  override val message: String = "Unable to extract plugin zip file ${pluginFile.name}"
}