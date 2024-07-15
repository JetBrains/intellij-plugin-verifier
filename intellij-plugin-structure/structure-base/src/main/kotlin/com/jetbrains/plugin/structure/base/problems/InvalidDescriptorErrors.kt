/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.problems


/**
 * Indicates an issue with plugin descriptor (`plugin.xml`).
 *
 * Such kinds of errors are treated in a special way.
 * Although they are indicated as [errors][PluginProblem.Level.ERROR],
 * they do not prevent successful creation of plugin by [com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager].
 */
abstract class InvalidDescriptorProblem(
  private val descriptorPath: String?,
  private val detailedMessage: String
) : PluginProblem() {
  final override val message: String
    get() = "Invalid plugin descriptor" +
            (" '$descriptorPath'.".takeIf { descriptorPath.isNullOrBlank().not() } ?: ".") +
            (" $detailedMessage".takeIf { detailedMessage.isNotBlank() } ?: "")
}

class InvalidPluginIDProblem(id: String) : InvalidDescriptorProblem(
  descriptorPath = "id",
  detailedMessage = "The plugin id contains unsupported symbols: $id."
) {
  override val level
    get() = Level.ERROR
}

class UnexpectedDescriptorElements(
  lineNumber: Int,
  descriptorPath: String? = null
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "There " + if (lineNumber == -1) "are unexpected elements." else "is an unexpected element on line $lineNumber."
) {
  override val level
    get() = Level.ERROR
}

class TooLongPropertyValue(
  descriptorPath: String,
  propertyName: String,
  propertyValueLength: Int,
  maxLength: Int
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The value of the '$propertyName' parameter is too long. Its length is $propertyValueLength " +
                    "which is more than maximum $maxLength characters long."
) {
  override val level
    get() = Level.ERROR
}

open class PropertyNotSpecified(
  propertyName: String,
  descriptorPath: String? = null
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The property <$propertyName> is not specified."
) {
  override val level
    get() = Level.ERROR
}

class NotBoolean(
  propertyName: String,
  descriptorPath: String? = null
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The property <$propertyName> value must be boolean."
) {
  override val level
    get() = Level.ERROR
}

class NotNumber(
  propertyName: String,
  descriptorPath: String? = null
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The property <$propertyName> value must be integer."
) {
  override val level
    get() = Level.ERROR
}

class UnableToReadDescriptor(
  descriptorPath: String,
  exceptionMessage: String?
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "Unable to read the plugin descriptor" + (exceptionMessage?.let { ": $it." } ?: ".")
) {

  override val level
    get() = Level.ERROR
}

class ContainsNewlines(propertyName: String, descriptorPath: String? = null) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The property <$propertyName> must not contain newlines."
) {
  override val level = Level.ERROR
}

class ReusedDescriptorInMultipleDependencies(
  descriptorPath: String? = null,
  configFile: String,
  val dependencies: List<String> = listOf()
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "Multiple dependencies (${dependencies.size}) use the same config-file attribute value " +
                    "'$configFile': ${dependencies.joinToString(prefix = "[", postfix = "]")}."
) {
  override val level: Level
    get() = Level.WARNING
}

class VendorCannotBeEmpty(
  descriptorPath: String? = null
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The property <vendor> value is empty. The vendor name or organization ID must be set. " +
                    "Example: <vendor>Joe Doe</vendor>."
) {

  override val hint = ProblemSolutionHint(
    example = "<vendor>Joe Doe</vendor>",
    documentationUrl = "https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html#idea-plugin__vendor"
  )

  override val level
    get() = Level.ERROR
}

class InvalidSemverFormat(
  descriptorPath: String,
  versionName: String,
  version: String
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The `$versionName` version should be formatted as SemVer [$version]."
) {
  override val level
    get() = Level.ERROR
}

class InvalidVersionRange(
  descriptorPath: String,
  since: String,
  until: String
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The compatibility range [$since, $until] is empty."
) {
  override val level
    get() = Level.ERROR
}

class SemverComponentLimitExceeded(
  descriptorPath: String,
  componentName: String,
  versionName: String,
  version: String,
  limit: Int
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The `$componentName` component of the `$versionName` SemVer version is too big [$version]. Max value is $limit."
) {
  override val level: Level
    get() = Level.ERROR
}