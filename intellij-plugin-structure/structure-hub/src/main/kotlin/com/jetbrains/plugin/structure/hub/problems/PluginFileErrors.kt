package com.jetbrains.plugin.structure.hub.problems

import com.jetbrains.plugin.structure.base.problems.IncorrectPluginFile
import com.jetbrains.plugin.structure.base.problems.PluginFileError

fun createIncorrectHubPluginFile(fileName: String): PluginFileError =
  IncorrectPluginFile(fileName, ".zip archive.")
