package com.jetbrains.plugin.structure.base.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

abstract class InvalidDescriptorProblem(descriptorPath: String?, detailedMessage: String) : PluginProblem() {
  override val message = if (descriptorPath != null)
    "Invalid plugin descriptor $descriptorPath: $detailedMessage" else
    "Invalid plugin descriptor: $detailedMessage"
}

class UnexpectedDescriptorElements(
    errorMessage: String,
    descriptorPath: String? = null
) : InvalidDescriptorProblem(descriptorPath, errorMessage) {

  override val level
    get() = PluginProblem.Level.ERROR

}

class PropertyNotSpecified(
    propertyName: String,
    descriptorPath: String? = null
) : InvalidDescriptorProblem(descriptorPath, "<$propertyName> is not specified") {

  override val level
    get() = Level.ERROR
}