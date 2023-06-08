package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.ContainsNewlines
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager.Companion.PLUGIN_XML
import com.jetbrains.plugin.structure.intellij.problems.TooLongPropertyValue

const val MAX_PROPERTY_LENGTH = 255

fun verifyNewlines(propertyName: String, propertyValue: String,
                   descriptorPath: String = PLUGIN_XML,
                   problemRegistrar: ProblemRegistrar) {
  if (propertyValue.trim().contains("\n")) {
    problemRegistrar.registerProblem(ContainsNewlines(propertyName, descriptorPath))
  }
}

fun verifyPropertyLength(propertyName: String, propertyValue: String, maxLength: Int,
                         descriptorPath: String = PLUGIN_XML, problemRegistrar: ProblemRegistrar) {
  if (propertyValue.length > maxLength) {
    problemRegistrar.registerProblem(TooLongPropertyValue(descriptorPath, propertyName, propertyValue.length, maxLength))
  }
}
