package com.jetbrains.plugin.structure.teamcity.action

import com.jetbrains.plugin.structure.base.problems.PluginProblem

abstract class InvalidPropertyProblem : PluginProblem() {
  override val level: Level = Level.ERROR
}

object ParseYamlProblem : InvalidPropertyProblem() {
  override val message = "The action specification should follow valid YAML syntax."
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

class PropertiesCombinationProblem(override val message: String) : InvalidPropertyProblem()