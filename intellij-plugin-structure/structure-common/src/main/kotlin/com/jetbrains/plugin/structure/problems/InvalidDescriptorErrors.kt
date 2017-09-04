package com.jetbrains.plugin.structure.problems

import com.jetbrains.plugin.structure.plugin.PluginProblem

abstract class InvalidDescriptorProblem(descriptorPath: String?, detailedMessage: String) : PluginProblem() {
  override val message: String = if (descriptorPath != null)
    "Invalid plugin descriptor $descriptorPath: $detailedMessage" else
    "Invalid plugin descriptor: $detailedMessage"
}

data class UnexpectedDescriptorElements(val errorMessage: String, val descriptorPath: String? = null) : InvalidDescriptorProblem(descriptorPath, errorMessage) {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR

}

data class PropertyNotSpecified(val propertyName: String, val descriptorPath: String? = null) :
    InvalidDescriptorProblem(descriptorPath, "<$propertyName> is not specified") {
  override val level = Level.ERROR
}