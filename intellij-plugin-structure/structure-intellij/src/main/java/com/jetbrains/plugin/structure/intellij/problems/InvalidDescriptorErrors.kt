package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.intellij.version.IdeVersion

class PropertyWithDefaultValue(descriptorPath: String, propertyName: String) : InvalidDescriptorProblem(descriptorPath, "<$propertyName> has default value") {
  override val level
    get() = PluginProblem.Level.ERROR
}

class InvalidDependencyBean(descriptorPath: String) : InvalidDescriptorProblem(descriptorPath, "dependency id is not specified") {
  override val level
    get() = PluginProblem.Level.ERROR
}

class InvalidModuleBean(descriptorPath: String) : InvalidDescriptorProblem(descriptorPath, "module is empty. It must be specified as <module value=\"my.module\"/>") {
  override val level
    get() = PluginProblem.Level.ERROR
}

class SinceBuildNotSpecified(descriptorPath: String) : InvalidDescriptorProblem(descriptorPath, "since build is not specified") {
  override val level
    get() = PluginProblem.Level.ERROR
}

class InvalidSinceBuild(
    descriptorPath: String,
    sinceBuild: String
) : InvalidDescriptorProblem(descriptorPath, "invalid since build: $sinceBuild") {
  override val level
    get() = PluginProblem.Level.ERROR
}

class InvalidUntilBuild(
    descriptorPath: String,
    untilBuild: String
) : InvalidDescriptorProblem(descriptorPath, "invalid until build: $untilBuild") {
  override val level
    get() = PluginProblem.Level.ERROR
}

class SinceBuildGreaterThanUntilBuild(
    descriptorPath: String,
    sinceBuild: IdeVersion,
    untilBuild: IdeVersion
) : InvalidDescriptorProblem(descriptorPath, "since build $sinceBuild is greater than until build $untilBuild") {
  override val level
    get() = PluginProblem.Level.ERROR
}

//todo: provide unresolved <x-include> names
class UnresolvedXIncludeElements(descriptorPath: String) : InvalidDescriptorProblem(descriptorPath, "unresolved xinclude elements") {
  override val level
    get() = PluginProblem.Level.ERROR
}

class TooLongPropertyValue(
    descriptorPath: String,
    propertyName: String,
    propertyValueLength: Int,
    maxLength: Int
) : InvalidDescriptorProblem(descriptorPath, "value of property '$propertyName' is too long. Its length is $propertyValueLength which is more than maximum $maxLength characters long") {
  override val level
    get() = PluginProblem.Level.ERROR
}