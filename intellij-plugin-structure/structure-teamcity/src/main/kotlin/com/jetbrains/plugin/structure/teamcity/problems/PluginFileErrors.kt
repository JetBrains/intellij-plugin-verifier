package com.jetbrains.plugin.structure.teamcity.problems

import com.jetbrains.plugin.structure.base.problems.IncorrectPluginFile
import java.io.File

data class IncorrectTeamCityPluginFile(val file: File) :
    IncorrectPluginFile(file, ".zip archive.")
