/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorResolutionError
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.ProblemSolutionHint
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.intellij.version.ProductReleaseVersion

class NoModuleDependencies(descriptorPath: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The plugin configuration file does not include any module dependency tags. So, the plugin is " +
                    "assumed to be a legacy plugin and is loaded only in IntelliJ IDEA. Please note that plugins should " +
                    "declare a dependency on `com.intellij.modules.platform` to indicate dependence on shared functionality."
) {
  override val level
    get() = Level.WARNING

  override val hint = ProblemSolutionHint(
    documentationUrl = "https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html"
  )
}

class DefaultChangeNotes(descriptorPath: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The change-notes parameter contains the default value 'Add change notes here' or 'most HTML tags may be used'."
) {
  override val level
    get() = Level.WARNING
}

class TemplateWordInPluginName(
  descriptorPath: String,
  pluginName: String,
  templateWord: String
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The plugin name '$pluginName' should not include the word '$templateWord'."
) {
  override val level
    get() = Level.WARNING
}

class TemplateWordInPluginId(
  descriptorPath: String,
  pluginId: String,
  templateWord: String
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The plugin ID '$pluginId' should not include the word '$templateWord'."
) {
  override val level
    get() = Level.WARNING
}

class OptionalDependencyDescriptorResolutionProblem(
  private val dependencyId: String,
  private val configurationFile: String,
  private val errors: List<PluginProblem>
) : PluginProblem() {

  override val level
    get() = Level.WARNING

  override val message: String
    get() {
      val descriptorResolutionError = errors.filterIsInstance<PluginDescriptorResolutionError>().firstOrNull()
      val prefix = "The configuration file '$configurationFile' for optional dependency '$dependencyId'"
      return if (descriptorResolutionError != null) {
        "$prefix failed to be resolved: ${descriptorResolutionError.message}"
      } else {
        prefix + " is invalid: ${errors.joinToString { it.message }}"
      }
    }
}

class ModuleDescriptorResolutionProblem(
  private val moduleName: String,
  private val configurationFile: String,
  private val errors: List<PluginProblem>
) : PluginProblem() {

  override val level
    get() = Level.WARNING

  override val message: String
    get() {
      val descriptorResolutionError = errors.filterIsInstance<PluginDescriptorResolutionError>().firstOrNull()
      val prefix = "The configuration file '$configurationFile' for module '$moduleName'"
      return if (descriptorResolutionError != null) {
        "$prefix failed to be resolved: ${descriptorResolutionError.message}"
      } else {
        prefix + " is invalid: ${errors.joinToString { it.message }}"
      }
    }
}

data class DuplicatedDependencyWarning(val dependencyId: String) : PluginProblem() {
  override val level
    get() = Level.WARNING

  override val message: String
    get() = "There is a duplicated dependency on '$dependencyId'. Remove this dependency by updating the plugin.xml file."
}

class SuperfluousNonOptionalDependencyDeclaration(private val dependencyId: String) : PluginProblem() {
  override val level
    get() = Level.WARNING

  override val message
    get() = "Dependency declaration <depends optional=\"false\">$dependencyId</dependency> is superfluous. " +
            "Dependencies are mandatory by default. Update the plugin.xml file and remove optional=\"false\" attribute " +
            "from the dependency parameters."
}

class OptionalDependencyConfigFileNotSpecified(private val optionalDependencyId: String) : PluginProblem() {
  override val level
    get() = Level.WARNING

  override val message
    get() = "Optional dependency declaration on '$optionalDependencyId' should specify \"config-file\". Declare " +
            "config-file attribute in addition to optional dependency in the plugin.xml file."
}

class ElementAvailableOnlySinceNewerVersion(
  private val elementName: String,
  private val availableSinceBuild: IdeVersion,
  private val pluginSinceBuild: IdeVersion,
  private val pluginUntilBuild: IdeVersion?
) : PluginProblem() {
  override val level
    get() = Level.WARNING

  override val message
    get() = "The <$elementName> element is available only since ${availableSinceBuild.asStringWithoutProductCode()} " +
            "but the plugin can be installed in " +
      if (pluginUntilBuild != null) {
        pluginSinceBuild.asStringWithoutProductCode() + "—" + pluginUntilBuild.asStringWithoutProductCode() + "."
      } else {
        pluginSinceBuild.asStringWithoutProductCode() + "+."
      }
}

class ElementMissingAttribute(
  private val elementName: String,
  private val attributeName: String
) : PluginProblem() {
  override val level
    get() = Level.WARNING

  override val message
    get() = "The <$elementName> element must specify attribute $attributeName. To define an application-level listener, " +
            "add the <applicationListeners> section to your plugin.xml along with <topic> and <class> attributes."
}


open class SuspiciousUntilBuild(
  private val untilBuild: String,
  private val additionalMessage: String = ""
) : PluginProblem() {
  override val hint = ProblemSolutionHint(
    documentationUrl = "https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html"
  )
  override val message: String
    get() = "The <until-build> '$untilBuild' does not represent the actual build number. " +
            if (additionalMessage.isNotBlank()) "$additionalMessage " else "" +
            "If you want your plugin to be compatible with all future IDE versions, you can remove this attribute. " +
            "However, we highly recommend setting it to the latest available IDE version."

  override val level
    get() = Level.WARNING
}

open class NonexistentReleaseInUntilBuild(
  untilBuild: String,
  nonexistentRelease: String = ""
) : SuspiciousUntilBuild(untilBuild, "Version '$nonexistentRelease' does not exist")

class SuspiciousReleaseVersion(
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
    get() = Level.WARNING
}

class ForbiddenPluginIdPrefix(
  descriptorPath: String,
  pluginId: String,
  prefix: String
) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "The plugin ID '$pluginId' has a prefix '$prefix' that is not allowed."
) {
  override val level
    get() = Level.WARNING

  override val hint = ProblemSolutionHint(
    documentationUrl = "https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html#idea-plugin__id"
  )
}

class UnknownServiceClientValue(descriptorPath: String, serviceClient: String) : InvalidDescriptorProblem(
  descriptorPath = descriptorPath,
  detailedMessage = "Plugin has unknown service client value: '$serviceClient'"
) {
  override val level
    get() = Level.WARNING
}