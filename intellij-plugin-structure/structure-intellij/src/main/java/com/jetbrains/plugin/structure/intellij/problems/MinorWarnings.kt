package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

data class NoModuleDependencies(val descriptorPath: String) : PluginProblem() {
  override val level = PluginProblem.Level.WARNING

  override val message = "Plugin descriptor $descriptorPath does not include any module dependency tags. " +
      "The plugin is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA."
}

data class NonLatinDescription(val descriptorPath: String) : PluginProblem() {
  override val level = PluginProblem.Level.WARNING
  override val message = "Please make sure to provide the description in English"
}

data class ShortDescription(val descriptorPath: String) : PluginProblem() {
  override val level = PluginProblem.Level.WARNING
  override val message = "Description is too short"
}


data class DefaultChangeNotes(val descriptorPath: String) : PluginProblem() {

  override val level = PluginProblem.Level.WARNING

  override val message = "Default value in plugin descriptor $descriptorPath: <change-notes> shouldn't have 'Add change notes here' or 'most HTML tags may be used'"
}

data class DefaultDescription(val descriptorPath: String) : PluginProblem() {

  override val level = PluginProblem.Level.WARNING

  override val message = "Default value in plugin descriptor $descriptorPath: <description> shouldn't have 'Enter short description for your plugin here.' or 'most HTML tags may be used'"
}

data class ShortChangeNotes(val descriptorPath: String) : PluginProblem() {

  override val level = PluginProblem.Level.WARNING

  override val message = "Too short <change-notes> in plugin descriptor $descriptorPath"
}

data class PluginWordInPluginName(val descriptorPath: String) : PluginProblem() {
  override val level = PluginProblem.Level.WARNING

  override val message = "Plugin name specified in $descriptorPath should not contain the word 'plugin'"

}

data class MissingOptionalDependencyConfigurationFile(val descriptorPath: String,
                                                      val dependency: PluginDependency,
                                                      val configurationFile: String) : PluginProblem() {

  override val level = PluginProblem.Level.WARNING

  override val message = "Configuration file $configurationFile for the optional dependency ${dependency.id} is not found"
}