package com.jetbrains.plugin.structure.base.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

abstract class PluginFileError : PluginProblem() {
  override val level
    get() = Level.ERROR
}

class IncorrectPluginFile(
    private val fileName: String,
    private val expectedFileType: String
) : PluginFileError() {
  override val message
    get() = "Incorrect plugin file $fileName. Must be a $expectedFileType"
}

class UnableToExtractZip : PluginFileError() {
  override val message
    get() = "Unable to extract plugin zip file"
}