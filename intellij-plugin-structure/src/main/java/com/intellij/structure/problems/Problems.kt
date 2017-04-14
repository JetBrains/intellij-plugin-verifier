package com.intellij.structure.problems

import com.intellij.structure.plugin.PluginDependency
import java.io.File

interface PluginProblem {

  val level: Level

  val message: String

  enum class Level {
    ERROR,
    WARNING
  }

}

data class IncorrectPluginFile(val file: File) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Incorrect plugin file $file. Must be a .zip or .jar archive or a directory."

}

data class MissingOptionalDependency(val dependency: PluginDependency, val configurationFile: String) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.WARNING

  override val message: String = "Plugin's dependency $dependency configuration file $configurationFile is not resolved"

}

data class UnableToExtractZip(val pluginFile: File) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Unable to extract plugin zip file $pluginFile"

}

data class UnableToReadPluginClassFiles(val pluginFile: File) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Unable to read plugin class files: $pluginFile"

}

data class MultiplePluginDescriptorsInLibDirectory(val firstFileName: String,
                                                   val secondFileName: String) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Found multiple plugin descriptors in plugin/lib/$firstFileName and plugin/lib/$secondFileName. Only one plugin must be bundled in a plugin distribution."

}


data class PluginDescriptorIsNotFound(val descriptorPath: String) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Plugin descriptor $descriptorPath is not found"

}

data class PluginLibDirectoryIsEmpty(val libDirectory: File) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Plugin's directory $libDirectory must not be empty"

}

data class UnableToReadJarFile(val jarFile: File) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Unable to read jar file $jarFile"

}

data class UnableToReadDescriptor(val descriptorPath: String) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Unable to read plugin descriptor $descriptorPath"

}

data class PluginNameIsNotSpecified(val descriptorPath: String) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Invalid plugin descriptor $descriptorPath: <name> is not specified"

}

data class VersionIsNotSpecified(val descriptorPath: String) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Invalid plugin descriptor $descriptorPath: <version> is not specified"

}

data class VendorIsNotSpecified(val descriptorPath: String) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Invalid plugin descriptor $descriptorPath: <vendor> is not specified"

}

data class IdeaVersionIsNotSpecified(val descriptorPath: String) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Invalid plugin descriptor $descriptorPath: <idea-version> is not specified"

}

data class EmptyDescription(val descriptorPath: String) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Invalid plugin descriptor $descriptorPath: <description> is empty"

}

data class InvalidDependencyBean(val descriptorPath: String) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Invalid plugin descriptor $descriptorPath: dependency id is not specified"

}

data class InvalidModuleBean(val descriptorPath: String) : PluginProblem {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Invalid plugin descriptor $descriptorPath: module is not specified"

}