package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.ContainsNewlines
import com.jetbrains.plugin.structure.intellij.problems.TooLongPropertyValue

const val DEFAULT_DESCRIPTOR_PATH = "plugin.xml"

const val MAX_PROPERTY_LENGTH = 255

fun validateNewlines(propertyName: String, propertyValue: String,
                     descriptorPath: String = DEFAULT_DESCRIPTOR_PATH,
                     problemRegistrar: ProblemRegistrar) {
  if (propertyValue.trim().contains("\n")) {
    problemRegistrar.registerProblem(ContainsNewlines(propertyName, descriptorPath))
  }
}

fun validatePropertyLength(propertyName: String, propertyValue: String, maxLength: Int,
                           descriptorPath: String = DEFAULT_DESCRIPTOR_PATH, problemRegistrar: ProblemRegistrar) {
  if (propertyValue.length > maxLength) {
    problemRegistrar.registerProblem(TooLongPropertyValue(descriptorPath, propertyName, propertyValue.length, maxLength))
  }
}
