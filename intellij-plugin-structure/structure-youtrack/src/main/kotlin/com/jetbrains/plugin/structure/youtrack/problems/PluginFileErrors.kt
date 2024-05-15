package com.jetbrains.plugin.structure.youtrack.problems

import com.jetbrains.plugin.structure.base.problems.IncorrectPluginFile
import com.jetbrains.plugin.structure.base.problems.PluginFileError

fun createIncorrectYouTrackPluginFileError(fileName: String): PluginFileError =
  IncorrectPluginFile(fileName, ".zip archive.")
