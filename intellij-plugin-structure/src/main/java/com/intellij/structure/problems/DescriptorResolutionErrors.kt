package com.intellij.structure.problems

data class MultiplePluginDescriptorsInLibDirectory(val firstFileName: String,
                                                   val secondFileName: String) : PluginProblem() {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Found multiple plugin descriptors in plugin/lib/$firstFileName and plugin/lib/$secondFileName. Only one plugin must be bundled in a plugin distribution."

}


data class PluginDescriptorIsNotFound(val descriptorPath: String) : PluginProblem() {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Plugin descriptor $descriptorPath is not found"

}

data class UnableToResolveXIncludeElements(val descriptorPath: String) : PluginProblem() {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Unable to resolve x-include elements of descriptor $descriptorPath"

}

data class UnableToReadDescriptor(val descriptorPath: String) : PluginProblem() {

  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

  override val message: String = "Unable to read plugin descriptor $descriptorPath"

}