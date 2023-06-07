package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.plugin.CORE_PLUGIN_ID
import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.plugin.SPECIAL_IDEA_PLUGIN_ID
import com.jetbrains.plugin.structure.intellij.problems.IllegalPluginIdPrefix
import com.jetbrains.plugin.structure.intellij.problems.PropertyWithDefaultValue
import com.jetbrains.plugin.structure.intellij.problems.TemplateWordInPluginId

/*
com.example.X and others which are clearly not "real"
non-JB plugins: anything containing jetbrains other than "our" official plugin ID prefixes (com.intellij.X, org.jetbrains.X ?)
non-JB plugins: anything containing our product names e.g. intellij
 */
val DEFAULT_ILLEGAL_PREFIXES = listOf("com.example", "net.example", "org.example", "edu.example", "com.intellij", "org.jetbrains")

val PRODUCT_ID_RESTRICTED_WORDS = listOf(
  "clion",  "datagrip", "datalore", "dataspell", "dotcover", "dotmemory", "dotpeek", "dottrace", "fleet", "goland",
  "intellij", "phpstorm", "pycharm", "resharper", "rider", "rubymine", "space", "webstorm", "youtrack",
)

class PluginIdVerifier {

  fun verify(plugin: PluginBean, descriptorPath: String, problemConsumer: (PluginProblem) -> Unit) {
    val id = plugin.id ?: return

    when {
      id.isBlank() -> {
        problemConsumer(PropertyNotSpecified("id"))
      }
      "com.your.company.unique.plugin.id" == id -> {
        problemConsumer(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.ID, id))
      }
      else -> {
        validatePropertyLength("id", id, MAX_PROPERTY_LENGTH) {
          problemConsumer(it)
        }
        validateNewlines("id", id) {
          problemConsumer(it)
        }
        verifyPrefix(plugin, descriptorPath, problemConsumer)
      }
    }
  }

  private fun verifyPrefix(plugin: PluginBean, descriptorPath: String, problemConsumer: (PluginProblem) -> Unit) {
    if (isDevelopedByJetBrains(plugin)) {
      return
    }
    val id = plugin.id
    DEFAULT_ILLEGAL_PREFIXES
      .filter(id::startsWith)
      .forEach { problemConsumer(IllegalPluginIdPrefix(id, it)) }

    id.split('.')
      .filter { idComponent -> PRODUCT_ID_RESTRICTED_WORDS.contains(idComponent) }
      .forEach { idComponent -> problemConsumer(TemplateWordInPluginId(descriptorPath, idComponent)) }
  }

  private fun isDevelopedByJetBrains(plugin: PluginBean): Boolean {
    return CORE_PLUGIN_ID == plugin.id ||
      SPECIAL_IDEA_PLUGIN_ID == plugin.id
  }

}