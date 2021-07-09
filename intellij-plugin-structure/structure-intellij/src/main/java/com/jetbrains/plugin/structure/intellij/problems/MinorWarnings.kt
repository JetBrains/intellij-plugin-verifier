/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorResolutionError
import com.jetbrains.plugin.structure.intellij.version.IdeVersion

class NoModuleDependencies(private val descriptorPath: String) : PluginProblem() {
  override val level
    get() = Level.WARNING

  override val message
    get() = "Plugin descriptor $descriptorPath does not include any module dependency tags. " +
      "The plugin is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA. " +
      "See https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/plugin_compatibility.html"
}

class NonLatinDescription : PluginProblem() {

  override val level
    get() = Level.WARNING

  override val message
    get() = "Please make sure to provide the description in English"
}

class ShortDescription : PluginProblem() {

  override val level
    get() = Level.WARNING

  override val message
    get() = "Description is too short"
}


class DefaultChangeNotes(private val descriptorPath: String) : PluginProblem() {

  override val level
    get() = Level.WARNING

  override val message
    get() = "Default value in plugin descriptor $descriptorPath: <change-notes> shouldn't have 'Add change notes here' or 'most HTML tags may be used'"
}

class TemplateWordInPluginName(private val descriptorPath: String, private val templateWord: String) : PluginProblem() {

  override val level
    get() = Level.WARNING

  override val message
    get() = "Plugin name specified in $descriptorPath should not contain the word '$templateWord'"

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
      val prefix = "Configuration file '$configurationFile' for optional dependency '$dependencyId'"
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
    get() = "Duplicated dependency on '$dependencyId'"
}

class SuperfluousNonOptionalDependencyDeclaration(private val dependencyId: String) : PluginProblem() {
  override val level
    get() = Level.WARNING

  override val message
    get() = "Dependency declaration <depends optional=\"false\">$dependencyId</dependency> is superfluous. Dependencies are mandatory by default."
}

class OptionalDependencyConfigFileNotSpecified(private val optionalDependencyId: String) : PluginProblem() {
  override val level
    get() = Level.WARNING

  override val message
    get() = "Optional dependency declaration on '$optionalDependencyId' should specify \"config-file\""
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
    get() = "Element <$elementName> is available only since ${availableSinceBuild.asStringWithoutProductCode()} but the plugin can be installed in " +
      if (pluginUntilBuild != null) {
        pluginSinceBuild.asStringWithoutProductCode() + "—" + pluginUntilBuild.asStringWithoutProductCode()
      } else {
        pluginSinceBuild.asStringWithoutProductCode() + "+"
      }
}

class ElementMissingAttribute(
  private val elementName: String,
  private val attributeName: String
) : PluginProblem() {
  override val level
    get() = Level.WARNING

  override val message
    get() = "Element <$elementName> must specify attribute $attributeName"
}


class SuspiciousUntilBuild(
  private val untilBuild: String
) : PluginProblem() {
  override val message: String
    get() = "Probably incorrect until build value: $untilBuild. If you want your plugin to be compatible with all future IDEs, you can leave this field empty. " +
      "For detailed info refer to https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html"

  override val level
    get() = Level.WARNING
}