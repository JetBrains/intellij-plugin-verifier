package com.jetbrains.plugin.structure.problems

import com.jetbrains.plugin.structure.plugin.PluginProblem
import java.io.File

data class PluginZipIsEmpty(val pluginZip: File) : PluginProblem() {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Plugin .zip file ${pluginZip.name} is empty"

}

data class PluginZipContainsUnknownFile(val pluginZip: File, val fileName: String) : PluginProblem() {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Plugin .zip file ${pluginZip.name} contains invalid file $fileName"

}

data class UnableToReadJarFile(val jarFile: File) : PluginProblem() {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Unable to read jar file ${jarFile.name}"

}

data class PluginLibDirectoryIsEmpty(val libDirectory: File) : PluginProblem() {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Plugin's directory ${libDirectory.name} must not be empty"

}