package com.jetbrains.plugin.structure.base.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

class PluginDescriptorIsNotFound(descriptorPath: String) : PluginProblem() {
  override val level
    get() = Level.ERROR

  override val message = "Plugin descriptor $descriptorPath is not found"
}

class UnableToReadDescriptor(descriptorPath: String? = null) : PluginProblem() {
  override val level
    get() = Level.ERROR

  override val message = if (descriptorPath != null)
    "Unable to read plugin descriptor $descriptorPath" else
    "Unable to read plugin descriptor"
}