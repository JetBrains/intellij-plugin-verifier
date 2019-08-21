package com.jetbrains.plugin.structure.dotnet.problems

import com.jetbrains.plugin.structure.base.problems.IncorrectPluginFile
import com.jetbrains.plugin.structure.base.problems.PluginFileError

fun createIncorrectDotNetPluginFileProblem(fileName: String): PluginFileError =
    IncorrectPluginFile(fileName, ".nupkg archive.")