package com.intellij.structure.problems

abstract class InvalidDescriptorProblem(descriptorPath: String, detailedMessage: String) : PluginProblem() {
  override val message: String = "Invalid plugin descriptor $descriptorPath: $detailedMessage"
}

data class PropertyNotSpecified(val descriptorPath: String, val propertyName: String) :
    InvalidDescriptorProblem(descriptorPath, "<$propertyName> is not specified") {
  override val level = PluginProblem.Level.ERROR
}

data class PropertyWithDefaultValue(val descriptorPath: String, val propertyName: String) :
    InvalidDescriptorProblem(descriptorPath, "<$propertyName> has default value") {
  override val level = PluginProblem.Level.ERROR
}

data class EmptyDescription(val descriptorPath: String) :
    InvalidDescriptorProblem(descriptorPath, "<description> is empty") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class InvalidDependencyBean(val descriptorPath: String) :
    InvalidDescriptorProblem(descriptorPath, "dependency id is not specified") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class InvalidModuleBean(val descriptorPath: String) :
    InvalidDescriptorProblem(descriptorPath, "module is not specified") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class SinceBuildNotSpecified(val descriptorPath: String) :
    InvalidDescriptorProblem(descriptorPath, "since build not specified") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class InvalidSinceBuild(val descriptorPath: String, val sinceBuild: String) :
    InvalidDescriptorProblem(descriptorPath, "invalid since build: $sinceBuild") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}

data class InvalidUntilBuild(val descriptorPath: String, val untilBuild: String) :
    InvalidDescriptorProblem(descriptorPath, "invalid until build: $untilBuild") {
  override val level: PluginProblem.Level = PluginProblem.Level.ERROR
}