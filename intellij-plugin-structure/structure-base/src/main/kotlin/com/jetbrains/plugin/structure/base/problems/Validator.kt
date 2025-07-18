package com.jetbrains.plugin.structure.base.problems

val ALLOWED_NAME_SYMBOLS = Regex("[a-zA-Z0-9 .,+_\\-/:()#'&\\[\\]|]+")

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

fun validatePluginNameIsCorrect(descriptor: String, name: String, problems: MutableList<PluginProblem>) {
  validatePluginNameIsCorrect(descriptor, name)?.let {
    problems.add(it)
  }
}

fun validatePluginNameIsCorrect(descriptor: String, name: String): PluginProblem? =
  if (!name.matches(ALLOWED_NAME_SYMBOLS)) {
    InvalidPluginName(descriptor, name)
  } else {
    null
  }