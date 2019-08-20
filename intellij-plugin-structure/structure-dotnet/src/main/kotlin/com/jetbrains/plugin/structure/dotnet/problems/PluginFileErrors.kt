package com.jetbrains.plugin.structure.dotnet.problems

import com.jetbrains.plugin.structure.base.problems.IncorrectPluginFile
import com.jetbrains.plugin.structure.base.problems.PluginFileError

fun createIncorrectDotNetPluginFileProblem(fileName: String): PluginFileError =
    IncorrectPluginFile(fileName, ".nupkg archive.")

class ReSharperPluginTooLargeError : PluginFileError() {
  override val message
    get() = "Plugin is larger than allowed"
}