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

abstract class InvalidDescriptorProblem(detailedMessage: String) : PluginProblem {
  abstract val descriptorPath: String
  override val message: String = "Invalid plugin descriptor $descriptorPath: $detailedMessage"
}

data class PluginNameIsNotSpecified(override val descriptorPath: String) :
    InvalidDescriptorProblem("<name> is not specified") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class VersionIsNotSpecified(override val descriptorPath: String) :
    InvalidDescriptorProblem("<version> is not specified") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class VendorIsNotSpecified(override val descriptorPath: String) :
    InvalidDescriptorProblem("<vendor> is not specified") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class IdeaVersionIsNotSpecified(override val descriptorPath: String) :
    InvalidDescriptorProblem("<idea-version> is not specified") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class EmptyDescription(override val descriptorPath: String) :
    InvalidDescriptorProblem("<description> is empty") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class InvalidDependencyBean(override val descriptorPath: String) :
  InvalidDescriptorProblem("dependency id is not specified"){
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class InvalidModuleBean(override val descriptorPath: String) :
    InvalidDescriptorProblem("module is not specified") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class SinceBuildNotSpecified(override val descriptorPath: String) :
    InvalidDescriptorProblem("since build not specified") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class InvalidSinceBuild(override val descriptorPath: String) :
    InvalidDescriptorProblem("invalid since build") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class InvalidUntilBuild(override val descriptorPath: String) :
    InvalidDescriptorProblem("invalid since build") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}