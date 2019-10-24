package com.jetbrains.plugin.structure.teamcity.problems

import com.jetbrains.plugin.structure.base.problems.IncorrectPluginFile
import com.jetbrains.plugin.structure.base.problems.PluginFileError

fun createIncorrectTeamCityPluginFile(fileName: String): PluginFileError =
  IncorrectPluginFile(fileName, ".zip archive.")