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

private val PLUGIN_NAME_SYMBOLS = "^[A-Za-z\\d_. ]+$".toRegex()
fun validatePluginName(
  descriptor: String,
  name: String?,
  problems: MutableList<PluginProblem>,
  propertyName: String = "name",
) {
  if (name.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(propertyName))
    return
  }
  if (!PLUGIN_NAME_SYMBOLS.matches(name)) {
    problems.add(InvalidPluginNameProblem(name))
  }
  validatePropertyLength(
    descriptor = descriptor,
    propertyName = propertyName,
    propertyValue = name,
    maxLength = MAX_NAME_LENGTH,
    problems = problems
  )
}