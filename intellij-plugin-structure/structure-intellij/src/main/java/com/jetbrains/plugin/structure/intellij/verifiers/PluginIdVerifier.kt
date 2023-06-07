package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.problems.IllegalPluginIdPrefix
import com.jetbrains.plugin.structure.intellij.problems.PropertyWithDefaultValue
import com.jetbrains.plugin.structure.intellij.problems.TemplateWordInPluginId

val DEFAULT_ILLEGAL_PREFIXES = listOf("com.example", "net.example", "org.example", "edu.example", "com.intellij", "org.jetbrains")

val JETBRAINS_VENDORS = listOf("JetBrains", "JetBrains s.r.o.")

val PRODUCT_ID_RESTRICTED_WORDS = listOf(
  "clion",  "datagrip", "datalore", "dataspell", "dotcover", "dotmemory", "dotpeek", "dottrace", "fleet", "goland",
  "intellij", "phpstorm", "pycharm", "resharper", "rider", "rubymine", "space", "webstorm", "youtrack",
)

class PluginIdVerifier {

  fun verify(plugin: PluginBean, descriptorPath: String, problemRegistrar: ProblemRegistrar) {
    val id = plugin.id ?: return

    when {
      id.isBlank() -> {
        problemRegistrar.registerProblem(PropertyNotSpecified("id"))
      }
      "com.your.company.unique.plugin.id" == id -> {
        problemRegistrar.registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.ID, id))
      }
      else -> {
        validatePropertyLength("id", id, MAX_PROPERTY_LENGTH, descriptorPath, problemRegistrar)
        validateNewlines("id", id, descriptorPath, problemRegistrar)
        verifyPrefix(plugin, descriptorPath, problemRegistrar)
      }
    }
  }

  private fun verifyPrefix(plugin: PluginBean, descriptorPath: String, problemRegistrar: ProblemRegistrar) {
    if (isDevelopedByJetBrains(plugin)) {
      return
    }
    val id = plugin.id
    DEFAULT_ILLEGAL_PREFIXES
      .filter(id::startsWith)
      .forEach { problemRegistrar.registerProblem(IllegalPluginIdPrefix(id, it)) }

    id.split('.')
      .filter { idComponent -> PRODUCT_ID_RESTRICTED_WORDS.contains(idComponent) }
      .forEach { idComponent -> problemRegistrar.registerProblem(TemplateWordInPluginId(descriptorPath, idComponent)) }
  }

  private fun isDevelopedByJetBrains(plugin: PluginBean): Boolean {
    return JETBRAINS_VENDORS.contains(plugin.vendor.name)
  }

}