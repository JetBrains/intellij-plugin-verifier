package com.jetbrains.plugin.structure.base.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

open class IncorrectPluginFile(
    private val fileName: String,
    private val expectedFileType: String
) : PluginProblem() {
  override val level
    get() = Level.ERROR

  override val message
    get() = "Incorrect plugin file $fileName. Must be a $expectedFileType"
}

class UnableToExtractZip : PluginProblem() {

  override val level
    get() = Level.ERROR

  override val message
    get() = "Unable to extract plugin zip file"
}