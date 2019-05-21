package com.jetbrains.plugin.structure.base.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

class PluginDescriptorIsNotFound(private val descriptorPath: String) : PluginProblem() {
  override val level
    get() = Level.ERROR

  override val message
    get() = "Plugin descriptor $descriptorPath is not found"
}

class UnableToReadDescriptor(private val descriptorPath: String? = null) : PluginProblem() {
  override val level
    get() = Level.ERROR

  override val message
    get() = "Unable to read plugin descriptor" + (if (descriptorPath != null) " $descriptorPath" else "")
}

class MultiplePluginDescriptorsInDistribution(
    private val firstDescriptorPath: String,
    private val secondDescriptorPath: String
) : PluginProblem() {
  override val level
    get() = PluginProblem.Level.ERROR

  override val message
    get() = "Found multiple plugin descriptors '${minOf(firstDescriptorPath, secondDescriptorPath)}' and '${maxOf(firstDescriptorPath, secondDescriptorPath)}'. Only one plugin must be bundled in a plugin distribution."
}
