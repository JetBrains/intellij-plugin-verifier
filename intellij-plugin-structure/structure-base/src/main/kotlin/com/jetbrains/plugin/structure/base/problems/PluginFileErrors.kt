package com.jetbrains.plugin.structure.base.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

open class IncorrectPluginFile(fileName: String, expectedFileType: String) : PluginProblem() {
  override val level
    get() = Level.ERROR

  override val message = "Incorrect plugin file $fileName. Must be a $expectedFileType"
}

class UnableToExtractZip(fileName: String) : PluginProblem() {

  override val level
    get() = Level.ERROR

  override val message = "Unable to extract plugin zip file $fileName"
}