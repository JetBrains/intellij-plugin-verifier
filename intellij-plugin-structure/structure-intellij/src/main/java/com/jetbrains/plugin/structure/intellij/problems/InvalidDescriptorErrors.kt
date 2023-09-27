/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.intellij.version.IdeVersion

class PropertyWithDefaultValue(
  descriptorPath: String,
  private val property: DefaultProperty,
  private val value: String
) : InvalidDescriptorProblem(descriptorPath) {

  enum class DefaultProperty(val propertyName: String) {
    ID("<id>"),
    NAME("<name>"),
    VENDOR("<vendor>"),
    VENDOR_URL("<vendor url>"),
    VENDOR_EMAIL("<vendor email>"),
    DESCRIPTION("<description>")
  }

  override val detailedMessage: String
    get() = "${property.propertyName} must not be equal to default value: '$value'"

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
      "For detailed info refer to https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html"

  override val level: Level
    get() = Level.ERROR
}

class ErroneousUntilBuild(
  descriptorPath: String,
  val untilBuild: IdeVersion
) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "until build '$untilBuild' must match the multi-part build number format, for example '182.4132.789' or '182.*'. " +
      "For detailed info refer to https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html"

  override val level: Level
    get() = Level.ERROR
}

class ProductCodePrefixInBuild(descriptorPath: String) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "'since-build' and 'until-build' shouldn't contain product code prefix"

  override val level: Level
    get() = Level.ERROR
}

class XIncludeResolutionErrors(descriptorPath: String, private val details: String) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "failed to resolve <xi:include>. ${details.capitalize()}"

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

class UnableToFindTheme(descriptorPath: String, private val themePath: String) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "unable to find theme by path '$themePath'"

  override val level
    get() = Level.ERROR
}

class UnableToReadTheme(descriptorPath: String, private val themePath: String, private val details: String?) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "unable to read theme by path '$themePath'" + (details?.let { ": $details" } ?: "")

  override val level
    get() = Level.ERROR
}

class OptionalDependencyDescriptorCycleProblem(descriptorPath: String, private val cyclicPath: List<String>) : InvalidDescriptorProblem(descriptorPath) {
  override val level
    get() = Level.ERROR

  override val detailedMessage: String
    get() = "optional dependencies configuration files contain cycle: " + cyclicPath.joinToString(separator = " -> ")
}

/**
 * Indicates optional dependency with empty config file.
 *
 * Example violation:
 * ```
 * <depends optional="true" config-file="">
 *   com.intellij.optional.plugin.id
 * </depends>
 * ```
 *
 */
class OptionalDependencyConfigFileIsEmpty(private val optionalDependencyId: String, descriptorPath: String) : InvalidDescriptorProblem(descriptorPath) {
  override val level
    get() = Level.ERROR

  override val detailedMessage: String
    get() = "Optional dependency declaration on '$optionalDependencyId' cannot have empty \"config-file\""
}