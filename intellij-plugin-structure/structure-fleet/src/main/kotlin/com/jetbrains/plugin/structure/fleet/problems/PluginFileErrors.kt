package com.jetbrains.plugin.structure.fleet.problems

import com.jetbrains.plugin.structure.base.problems.IncorrectPluginFile
import com.jetbrains.plugin.structure.base.problems.PluginFileError

fun createIncorrectFleetPluginFile(fileName: String): PluginFileError =
  IncorrectPluginFile(fileName, ".zip archive.")
