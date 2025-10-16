/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.base.problems.ProblemSolutionHint
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.intellij.version.ProductReleaseVersion

class PropertyWithDefaultValue(
  descriptorPath: String,
  property: DefaultProperty,
  value: String
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "One of the parameters matches the default value. Please ensure that ${property.propertyName} " +
                    "is not equal to the default value '$value'."
) {
  enum class DefaultProperty(val propertyName: String) {
    ID("<id>"),
    NAME("<name>"),
    VENDOR("<vendor>"),
    VENDOR_URL("<vendor url>"),
    VENDOR_EMAIL("<vendor email>"),
    DESCRIPTION("<description>")
  }

  override val level
    get() = Level.ERROR
}

class InvalidDependencyId(descriptorPath: String, invalidPluginId: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The dependency ID is invalid. '${invalidPluginId.trim()}' cannot be empty and must not contain " +
                    "newline characters."
) {
  override val level
    get() = Level.ERROR
}

/**
 * @see [com.jetbrains.plugin.structure.intellij.verifiers.K2IdeModeCompatibilityVerifier]
 */
private const val INVALID_KOTLIN_PLUGIN_MODE_MESSAGE = "Plugin depends on the Kotlin plugin (org.jetbrains.kotlin)," +
  "but does not declare compatibility with either " +
  "K1 Mode or K2 mode in the <org.jetbrains.kotlin.supportsKotlinPluginMode> extension. Please ensure that the " +
  "'supportsK1' or 'supportsK2' parameter (or both) is set to 'true'. " +
  "This feature is available for IntelliJ IDEA 2024.2.1 or later."

class InvalidKotlinPluginMode(descriptorPath: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = INVALID_KOTLIN_PLUGIN_MODE_MESSAGE
) {
  override val level
    get() = Level.ERROR

  override val hint = ProblemSolutionHint(
    """<supportsKotlinPluginMode supportsK1="false" supportsK2="false" />""",
    "https://kotlin.github.io/analysis-api/migrating-from-k1.html#declaring-compatibility-with-the-k2-kotlin-mode"
  )
}

class InvalidModuleBean(descriptorPath: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The <module value> parameter is empty. It must be specified as <module value=\"my.module\"/>."
) {
  override val level
    get() = Level.ERROR
}

class SinceBuildNotSpecified(descriptorPath: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The <since-build> parameter is not specified in the plugin.xml file."
) {
  override val level
    get() = Level.ERROR
}

class InvalidSinceBuild(
  descriptorPath: String,
  sinceBuild: String
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The <since-build> parameter ($sinceBuild) format is invalid. Ensure it is greater than <130> " +
                    "and represents the actual build numbers."
) {
  override val level
    get() = Level.ERROR
}

open class InvalidUntilBuild(
  descriptorPath: String,
  untilBuild: String,
  untilBuildVersion: IdeVersion? = null,
  detailedMessage: String = "The <until-build> attribute ($untilBuild) does not match the multi-part build number format " +
    "such as <branch>.<build_number>.<version>, for example, '182.4132.789'."
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage
) {
  override val level
    get() = Level.ERROR

  override val hint = ProblemSolutionHint(
    example = "until-build=\"182.4132.789\"",
    documentationUrl = "https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html"
  )
}

class InvalidUntilBuildWithJustBranch(
  descriptorPath: String,
  untilBuild: String,
) : InvalidUntilBuild(
  untilBuild = untilBuild,
  descriptorPath = descriptorPath,
  detailedMessage = "The <until-build> attribute with only a branch number ($untilBuild) is not valid. " +
                    "To specify compatibility with a whole branch, include a wildcard, for example '$untilBuild.*'."
) {
  override val level
    get() = Level.ERROR

  override val hint = ProblemSolutionHint(
    example = "until-build=\"241.*\"",
    documentationUrl = "https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html"
  )
}

class InvalidUntilBuildWithMagicNumber(
  descriptorPath: String,
  untilBuild: String,
  magicNumber: String
) : InvalidUntilBuild(
  descriptorPath = descriptorPath,
  untilBuild,
  detailedMessage = "The <until-build> attribute ($untilBuild) should not contain a magic value ($magicNumber). " +
    "If you want your plugin to be compatible with all future IDE versions, you can remove this attribute. " +
    "However, we highly recommend setting it to the latest available IDE version."
) {
  override val level
    get() = Level.ERROR

  override val hint = ProblemSolutionHint(
    example = "until-build=\"241.*\"",
    documentationUrl = "https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html"
  )
}

class SinceBuildGreaterThanUntilBuild(
  descriptorPath: String,
  sinceBuild: IdeVersion,
  untilBuild: IdeVersion
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The <since-build> parameter ($sinceBuild) must not be greater than the <until-build> parameter ($untilBuild)."
) {
  override val level
    get() = Level.ERROR
}

class SinceBuildCannotContainWildcard(
  descriptorPath: String,
  sinceBuild: IdeVersion,
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The <since-build> parameter ($sinceBuild) must not contain a wildcard (dot-star suffix) '.*')."
) {
  override val level
    get() = Level.WARNING

  override val hint = ProblemSolutionHint(
    example = "<since-build>241</vendor>",
    documentationUrl = "https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html"
  )
}

class ErroneousSinceBuild(
  descriptorPath: String,
  sinceBuild: IdeVersion
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The <since-build> parameter ($sinceBuild) does not match the multi-part build number format " +
                     "<branch>.<build_number>.<version>, for example, '182.4132.789'."
) {
  override val hint = ProblemSolutionHint(
    example = "since-build=\"182.4132.789\"",
    documentationUrl = "https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html"
  )

  override val level: Level
    get() = Level.ERROR
}

class ProductCodePrefixInBuild(descriptorPath: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The <since-build> and <until-build> parameters must not contain product code prefix."
) {
  override val level: Level
    get() = Level.ERROR
}

class XIncludeResolutionErrors(descriptorPath: String, error: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "Failed to resolve <xi:include> statement in the plugin.xml file. ${error.capitalize()}."
) {
  override val level
    get() = Level.ERROR
}

class ReleaseDateWrongFormat(descriptorPath: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The <release-date> parameter must be of YYYYMMDD format (type: integer)."
) {
  override val level
    get() = Level.ERROR
}

class ReleaseDateInFuture(descriptorPath: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The <release-date> parameter must be set to a date that is no more than five days in the future from today's date."
) {
  override val level
    get() = Level.ERROR
}

class ReleaseVersionWrongFormat(
  descriptorPath: String,
  releaseVersion: String
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The <release-version> parameter ($releaseVersion) format is invalid. " +
    "Ensure it is an integer with at least two digits."
) {
  override val level
    get() = Level.ERROR
}

class ReleaseVersionAndPluginVersionMismatch(
  descriptorPath: String,
  releaseVersion: ProductReleaseVersion,
  pluginVersion: String
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The <release-version> parameter [$releaseVersion] and the plugin version [$pluginVersion] " +
    "should have a matching beginning. " +
    "For example, release version '20201' should match plugin version 2020.1.1"
) {
  override val level
    get() = Level.ERROR

  override val hint: ProblemSolutionHint
    get() = ProblemSolutionHint(
      "release-version=\"20201\" and <version>2020.1</version>",
      "https://plugins.jetbrains.com/docs/marketplace/add-required-parameters.html"
    )
}

class UnableToFindTheme(descriptorPath: String, themePath: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The theme description file cannot be found by the path '$themePath'. Ensure the theme description " +
                    "file is present and follows the JSON open-standard file format of key-value pairs."
) {
  override val level
    get() = Level.ERROR
}

class UnableToReadTheme(descriptorPath: String, themePath: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The theme description file cannot be read from the path '$themePath'. Ensure the theme description " +
                    "file is present and follows the JSON open-standard file format of key-value pairs."
) {
  override val level
    get() = Level.ERROR
}

class OptionalDependencyDescriptorCycleProblem(
  descriptorPath: String,
  cyclicPath: List<String>
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The declared optional dependencies configuration files contain a cycle: " +
          cyclicPath.joinToString(separator = " -> ", postfix = ".")
) {
  override val level
    get() = Level.ERROR
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
class OptionalDependencyConfigFileIsEmpty(optionalDependencyId: String, descriptorPath: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "Optional dependency declaration on '$optionalDependencyId' cannot have empty \"config-file\"."
) {
  override val level
    get() = Level.ERROR
}

class DependencyConstraintsDuplicates(descriptorPath: String, modules: List<String>) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The plugin configuration file includes multiple module constraints of the same type. " +
    "Please remove the duplicate constraints: " + modules.joinToString(", ")
) {
  override val level
    get() = Level.ERROR
}