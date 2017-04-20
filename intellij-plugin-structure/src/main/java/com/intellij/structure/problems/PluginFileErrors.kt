package com.intellij.structure.problems

import java.io.File

data class IncorrectPluginFile(val file: File) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Incorrect plugin file $file. Must be a .zip or .jar archive or a directory."

}

data class UnableToExtractZip(val pluginFile: File) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Unable to extract plugin zip file $pluginFile"

}

data class UnableToReadPluginClassFiles(val pluginFile: File) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Unable to read plugin class files: $pluginFile"

}

data class UnableToReadJarFile(val jarFile: File) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Unable to read jar file $jarFile"

}

data class PluginLibDirectoryIsEmpty(val libDirectory: File) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Plugin's directory $libDirectory must not be empty"

}