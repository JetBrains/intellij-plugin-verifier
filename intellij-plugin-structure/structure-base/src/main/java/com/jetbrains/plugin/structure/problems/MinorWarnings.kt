package com.jetbrains.plugin.structure.problems

import com.jetbrains.plugin.structure.plugin.PluginDependency
import com.jetbrains.plugin.structure.plugin.PluginProblem

data class NoModuleDependencies(val descriptorPath: String) : PluginProblem() {
  override val level: PluginProblem.Level = PluginProblem.Level.WARNING

  override val message: String = "Plugin descriptor $descriptorPath does not include any module dependency tags. " +
      "The plugin is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA."
}

data class NonLatinDescription(val descriptorPath: String) : PluginProblem() {
  override val level: PluginProblem.Level = PluginProblem.Level.WARNING
  override val message: String = "Please make sure to provide the description in English"
}

data class ShortDescription(val descriptorPath: String) : PluginProblem() {
  override val level: PluginProblem.Level = PluginProblem.Level.WARNING
  override val message: String = "Description is too short"
}


data class DefaultChangeNotes(val descriptorPath: String) : PluginProblem() {

  override val level: PluginProblem.Level = PluginProblem.Level.WARNING

  override val message: String = "Default value in plugin descriptor $descriptorPath: <change-notes> shouldn't have 'Add change notes here' or 'most HTML tags may be used'"
}

data class DefaultDescription(val descriptorPath: String) : PluginProblem() {

  override val level: PluginProblem.Level = PluginProblem.Level.WARNING

  override val message: String = "Default value in plugin descriptor $descriptorPath: <description> shouldn't have 'Enter short description for your plugin here.' or 'most HTML tags may be used'"
}

data class ShortChangeNotes(val descriptorPath: String) : PluginProblem() {

  override val level: PluginProblem.Level = PluginProblem.Level.WARNING

  override val message: String = "Too short <change-notes> in plugin descriptor $descriptorPath"
}

data class PluginWordInPluginName(val descriptorPath: String) : PluginProblem() {
  override val level: PluginProblem.Level = PluginProblem.Level.WARNING

  override val message: String = "Plugin name specified in $descriptorPath should not contain the word 'plugin'"

}

data class MissingOptionalDependencyConfigurationFile(val descriptorPath: String, val dependency: PluginDependency, val configurationFile: String) :
    InvalidDescriptorProblem(descriptorPath, "configuration file $configurationFile of the dependency $dependency is not found") {

  override val level: PluginProblem.Level = PluginProblem.Level.WARNING
}