package com.jetbrains.plugin.structure.dotnet.problems

import com.jetbrains.plugin.structure.base.problems.IncorrectPluginFile

class IncorrectDotNetPluginFile(fileName: String) :
    IncorrectPluginFile(fileName, ".nupkg archive.")
