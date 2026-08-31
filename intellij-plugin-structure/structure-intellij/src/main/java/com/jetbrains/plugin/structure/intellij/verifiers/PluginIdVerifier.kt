package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.PropertyWithDefaultValue
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.problems.ForbiddenPluginIdPrefix
import com.jetbrains.plugin.structure.intellij.problems.TemplateWordInPluginId

val DEFAULT_ILLEGAL_PREFIXES = listOf("com.example", "net.example", "org.example", "edu.example", "com.intellij", "org.jetbrains")

val PRODUCT_ID_RESTRICTED_WORDS = listOf(
  "aqua", "clion",  "datagrip", "datalore",
  "dataspell", "dotcover", "dotmemory", "dotpeek", "dottrace", "fleet", "goland",
  "intellij", "qodana", "phpstorm", "pycharm", "resharper", "rider", "rubymine", "space", "webstorm", "youtrack",
)

class PluginIdVerifier {

  fun verify(plugin: PluginBean, descriptorPath: String, problemRegistrar: ProblemRegistrar) =
    verify(plugin.id, descriptorPath, problemRegistrar)

  /**
   * The `id` is all this verifier ever needed off the descriptor, so it is taken directly - which lets
   * both descriptor pipelines share it, [PluginBeanValidator][com.jetbrains.plugin.structure.intellij.plugin.PluginBeanValidator]
   * through the overload above and
   * [PlatformDescriptorValidator][com.jetbrains.plugin.structure.intellij.plugin.PlatformDescriptorValidator]
   * directly.
   */
  fun verify(id: String?, descriptorPath: String, problemRegistrar: ProblemRegistrar) {
    if (id == null) return

    when {
      id.isBlank() -> {
        problemRegistrar.registerProblem(PropertyNotSpecified("id"))
      }
      "com.your.company.unique.plugin.id" == id -> {
        problemRegistrar.registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.ID, id))
      }
      else -> {
        verifyPropertyLength("id", id, MAX_PROPERTY_LENGTH, descriptorPath, problemRegistrar)
        verifyNewlines("id", id, descriptorPath, problemRegistrar)
        verifyPrefix(id, descriptorPath, problemRegistrar)
      }
    }
  }

  private fun verifyPrefix(id: String, descriptorPath: String, problemRegistrar: ProblemRegistrar) {
    DEFAULT_ILLEGAL_PREFIXES
      .filter(id::startsWith)
      .forEach { problemRegistrar.registerProblem(ForbiddenPluginIdPrefix(descriptorPath, id, it)) }

    id.split('.')
      .filter { idComponent -> PRODUCT_ID_RESTRICTED_WORDS.contains(idComponent.lowercase()) }
      .forEach { idComponent -> problemRegistrar.registerProblem(TemplateWordInPluginId(descriptorPath, id, idComponent)) }
  }

}