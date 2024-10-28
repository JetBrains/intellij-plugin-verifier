package com.jetbrains.plugin.structure.teamcity.action

import com.jetbrains.plugin.structure.base.problems.PluginProblem

abstract class InvalidPropertyProblem : PluginProblem() {
  override val level: Level = Level.ERROR
}

object ParseYamlProblem : InvalidPropertyProblem() {
  override val message = "The action specification should follow valid YAML syntax."
}

data class UnknownPropertyProblem(val propertyName: String) : InvalidPropertyProblem() {
  override val message = "Unknown property <$propertyName>."
}

class InvalidPropertyValueProblem(override val message: String) : InvalidPropertyProblem()

class MissingValueProblem(propertyName: String, propertyDescription: String) : InvalidPropertyProblem() {
  override val message = "The property <$propertyName> ($propertyDescription) should be specified."
}

class EmptyValueProblem(propertyName: String, propertyDescription: String) : InvalidPropertyProblem() {
  override val message = "The property <$propertyName> ($propertyDescription) should not be empty."
}

class EmptyCollectionProblem(propertyName: String, propertyDescription: String) : InvalidPropertyProblem() {
  override val message = "The array property <$propertyName> ($propertyDescription) should have at least one value."
}

class InvalidBooleanProblem(propertyName: String, propertyDescription: String) : InvalidPropertyProblem() {
  override val message = "The property <$propertyName> ($propertyDescription) should be either 'true' or 'false'."
}

class InvalidNumberProblem(propertyName: String, propertyDescription: String) : InvalidPropertyProblem() {
  override val message = "The property <$propertyName> ($propertyDescription) should be a valid number."
}

class TooLongValueProblem(
  propertyName: String,
  propertyDescription: String,
  currentLength: Int,
  maxAllowedLength: Int,
) : InvalidPropertyProblem() {
  override val message =
    "The property <$propertyName> ($propertyDescription) should not contain more than $maxAllowedLength characters. " +
        "The current number of characters is $currentLength."
}

class TooShortValueProblem(
  propertyName: String,
  propertyDescription: String,
  currentLength: Int,
  minAllowedLength: Int,
) : InvalidPropertyProblem() {
  override val message =
    "The property <$propertyName> ($propertyDescription) should not be shorter than $minAllowedLength characters. " +
      "The current number of characters is $currentLength."
}

data class UnsupportedRunnerProblem(
  val runnerName: String,
  val supportedRunners: Collection<String>,
) : InvalidPropertyProblem() {
  override val message = "Unsupported runner $runnerName. Supported runners: ${supportedRunners.joinToString(", ")}"
}

data class UnsupportedRunnerParamsProblem(
  val runnerName: String,
  val unsupportedParams: Collection<String>,
  val supportedParams: Collection<String>,
) : InvalidPropertyProblem() {
  override val message: String
    get() {
      check(supportedParams.isNotEmpty())
      val paramsPrefix = when (unsupportedParams.size) {
        1 -> """Parameter "${unsupportedParams.first()}" is"""
        else -> "Parameters ${unsupportedParams.joinUsingDoubleQuotes()} are"
      }
      return "$paramsPrefix not supported by $runnerName runner. " +
          "Supported parameters: ${supportedParams.joinUsingDoubleQuotes()}"
    }

  private fun Collection<String>.joinUsingDoubleQuotes() =
    joinToString(prefix = "\"", separator = "\", \"", postfix = "\"")
}

class PropertiesCombinationProblem(override val message: String) : InvalidPropertyProblem()