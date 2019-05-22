package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

class NoModuleDependencies(val descriptorPath: String) : PluginProblem() {
  override val level
    get() = Level.WARNING

  override val message
    get() = "Plugin descriptor $descriptorPath does not include any module dependency tags. " +
        "The plugin is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA."
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

class ShortChangeNotes(private val descriptorPath: String) : PluginProblem() {

  override val level
    get() = Level.WARNING

  override val message
    get() = "Too short <change-notes> in plugin descriptor $descriptorPath"
}

class PluginWordInPluginName(private val descriptorPath: String) : PluginProblem() {

  override val level
    get() = Level.WARNING

  override val message
    get() = "Plugin name specified in $descriptorPath should not contain the word 'plugin'"

}

class OptionalDependencyDescriptorResolutionProblem(
    private val dependencyId: String,
    private val configurationFile: String,
    private val problems: List<PluginProblem>
) : PluginProblem() {

  override val level
    get() = Level.WARNING

  override val message
    get() = "Configuration file '$configurationFile' for optional dependency '$dependencyId' can't be resolved: ${problems.joinToString { it.message }}"
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
    get() = "optional dependency declaration on '$optionalDependencyId' should specify \"config-file\""
}
