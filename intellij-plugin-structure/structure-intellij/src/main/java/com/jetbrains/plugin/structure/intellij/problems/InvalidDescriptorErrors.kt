package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.intellij.version.IdeVersion

class PropertyWithDefaultValue(
    descriptorPath: String,
    private val defaultProperty: DefaultProperty
) : InvalidDescriptorProblem(descriptorPath) {

  enum class DefaultProperty(val propertyName: String, val defaultValue: String) {
    ID("<id>", "com.your.company.unique.plugin.id"),
    NAME("<name>", "Plugin display name here"),
    VENDOR("<vendor>", "YourCompany"),
    VENDOR_URL("<vendor url>", "https://www.yourcompany.com"),
    VENDOR_EMAIL("<vendor email>", "support@yourcompany.com")
  }

  override val detailedMessage: String
    get() = "${defaultProperty.propertyName} must not be equal to default value '${defaultProperty.defaultValue}'"

  override val level
    get() = Level.ERROR
}

class InvalidDependencyId(descriptorPath: String, private val invalidPluginId: String) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "dependency id is invalid: '${invalidPluginId.trim()}' cannot be empty and must not contain new line characters"

  override val level
    get() = Level.ERROR
}

class InvalidModuleBean(descriptorPath: String) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "module is empty. It must be specified as <module value=\"my.module\"/>"

  override val level
    get() = Level.ERROR
}

class SinceBuildNotSpecified(descriptorPath: String) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "since build is not specified"

  override val level
    get() = Level.ERROR
}

class InvalidSinceBuild(
    descriptorPath: String,
    private val sinceBuild: String
) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "invalid since build: $sinceBuild"

  override val level
    get() = Level.ERROR
}

class InvalidUntilBuild(
    descriptorPath: String,
    private val untilBuild: String
) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "invalid until build: $untilBuild"

  override val level
    get() = Level.ERROR
}

class SinceBuildGreaterThanUntilBuild(
    descriptorPath: String,
    private val sinceBuild: IdeVersion,
    private val untilBuild: IdeVersion
) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "since build $sinceBuild is greater than until build $untilBuild"

  override val level
    get() = Level.ERROR
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
    get() = Level.ERROR
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
    get() = Level.ERROR
}

class DefaultDescription(descriptorPath: String) : InvalidDescriptorProblem(descriptorPath) {

  override val level
    get() = Level.ERROR

  override val detailedMessage
    get() = "value of <description> must not have default 'Enter short description for your plugin here.' or 'most HTML tags may be used'"
}

object ReleaseDateWrongFormat : PluginProblem() {
  override val level
    get() = Level.ERROR

  override val message
    get() = "Property <release-date> must be of YYYYMMDD format"
}

class UnableToReadTheme(descriptorPath: String, private val themePath: String) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "unable to read theme by path '$themePath'"

  override val level
    get() = Level.ERROR
}
