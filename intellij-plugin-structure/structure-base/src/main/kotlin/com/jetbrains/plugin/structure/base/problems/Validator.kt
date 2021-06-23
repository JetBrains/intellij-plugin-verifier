package com.jetbrains.plugin.structure.base.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

fun validatePropertyLength(
  descriptor: String,
  propertyName: String,
  propertyValue: String,
  maxLength: Int,
  problems: MutableList<PluginProblem>
) {
  if (propertyValue.length > maxLength) {
    problems.add(TooLongPropertyValue(descriptor, propertyName, propertyValue.length, maxLength))
  }
}