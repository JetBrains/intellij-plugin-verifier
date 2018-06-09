package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

class NoModuleDependencies(descriptorPath: String) : PluginProblem() {
  override val level
    get() = PluginProblem.Level.WARNING

  override val message = "Plugin descriptor $descriptorPath does not include any module dependency tags. " +
      "The plugin is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA."
}

class NonLatinDescription : PluginProblem() {

  override val level
    get() = PluginProblem.Level.WARNING

  override val message = "Please make sure to provide the description in English"
}

class ShortDescription : PluginProblem() {

  override val level
    get() = PluginProblem.Level.WARNING

  override val message = "Description is too short"
}


class DefaultChangeNotes(descriptorPath: String) : PluginProblem() {

  override val level
    get() = PluginProblem.Level.WARNING

  override val message = "Default value in plugin descriptor $descriptorPath: <change-notes> shouldn't have 'Add change notes here' or 'most HTML tags may be used'"
}

class DefaultDescription(descriptorPath: String) : PluginProblem() {

  override val level
    get() = PluginProblem.Level.WARNING

  override val message = "Default value in plugin descriptor $descriptorPath: <description> shouldn't have 'Enter short description for your plugin here.' or 'most HTML tags may be used'"
}

class ShortChangeNotes(descriptorPath: String) : PluginProblem() {

  override val level
    get() = PluginProblem.Level.WARNING

  override val message = "Too short <change-notes> in plugin descriptor $descriptorPath"
}

class PluginWordInPluginName(descriptorPath: String) : PluginProblem() {

  override val level
    get() = PluginProblem.Level.WARNING

  override val message = "Plugin name specified in $descriptorPath should not contain the word 'plugin'"

}

class MissingOptionalDependencyConfigurationFile(
    configurationFile: String,
    dependencyId: String
) : PluginProblem() {

  override val level
    get() = PluginProblem.Level.WARNING

  override val message = "Configuration file $configurationFile for optional dependency $dependencyId is not found"
}