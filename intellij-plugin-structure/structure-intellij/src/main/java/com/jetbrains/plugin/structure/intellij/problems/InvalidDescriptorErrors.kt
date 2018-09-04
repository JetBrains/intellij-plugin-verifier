package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.intellij.version.IdeVersion

class PropertyWithDefaultValue(descriptorPath: String, private val propertyName: String) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "<$propertyName> has default value"

  override val level
    get() = PluginProblem.Level.ERROR
}

class InvalidDependencyBean(descriptorPath: String) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "dependency id is not specified"

  override val level
    get() = PluginProblem.Level.ERROR
}

class InvalidModuleBean(descriptorPath: String) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "module is empty. It must be specified as <module value=\"my.module\"/>"

  override val level
    get() = PluginProblem.Level.ERROR
}

class SinceBuildNotSpecified(descriptorPath: String) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "since build is not specified"

  override val level
    get() = PluginProblem.Level.ERROR
}

class InvalidSinceBuild(
    descriptorPath: String,
    private val sinceBuild: String
) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "invalid since build: $sinceBuild"

  override val level
    get() = PluginProblem.Level.ERROR
}

class InvalidUntilBuild(
    descriptorPath: String,
    private val untilBuild: String
) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "invalid until build: $untilBuild"

  override val level
    get() = PluginProblem.Level.ERROR
}

class SinceBuildGreaterThanUntilBuild(
    descriptorPath: String,
    private val sinceBuild: IdeVersion,
    private val untilBuild: IdeVersion
) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "since build $sinceBuild is greater than until build $untilBuild"

  override val level
    get() = PluginProblem.Level.ERROR
}

class ErroneousSinceBuild(
    descriptorPath: String,
    val sinceBuild: IdeVersion
) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "since build '$sinceBuild' must match the multi-part build number format '<branch>.<build_number>.<version>', for example '182.4132.789'. " +
        "For detailed info refer to https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html"

  override val level: Level
    get() = Level.ERROR
}

class ErroneousUntilBuild(
    descriptorPath: String,
    val untilBuild: IdeVersion
) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "until build '$untilBuild' must match the multi-part build number format, for example '182.4132.789' or '182.*'. " +
        "For detailed info refer to https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html"

  override val level: Level
    get() = Level.ERROR
}

//todo: provide unresolved <x-include> names
class UnresolvedXIncludeElements(descriptorPath: String) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "unresolved xinclude elements"

  override val level
    get() = PluginProblem.Level.ERROR
}

class TooLongPropertyValue(
    descriptorPath: String,
    private val propertyName: String,
    private val propertyValueLength: Int,
    private val maxLength: Int
) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "value of property '$propertyName' is too long. Its length is $propertyValueLength which is more than maximum $maxLength characters long"

  override val level
    get() = PluginProblem.Level.ERROR
}

class DefaultDescription(private val descriptorPath: String) : PluginProblem() {

  override val level
    get() = PluginProblem.Level.ERROR

  override val message
    get() = "Default value in plugin descriptor $descriptorPath: <description> shouldn't have 'Enter short description for your plugin here.' or 'most HTML tags may be used'"
}
