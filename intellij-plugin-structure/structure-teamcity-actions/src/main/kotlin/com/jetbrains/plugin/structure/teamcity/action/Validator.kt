package com.jetbrains.plugin.structure.teamcity.action

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionDescription
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputDefault
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputDescription
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputLabel
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputName
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputOptions
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputRequired
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputType
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionName
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionRequirementName
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionRequirementValue
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionSpecVersion
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionStepName
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionStepScript
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionStepWith
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionSteps
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionVersion
import com.jetbrains.plugin.structure.teamcity.action.model.ActionRequirementType
import com.jetbrains.plugin.structure.teamcity.action.problems.*
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.SemverException

internal fun validateTeamCityAction(descriptor: TeamCityActionDescriptor) = sequence {
  validateExistsAndNotEmpty(descriptor.specVersion, ActionSpecVersion.NAME, ActionSpecVersion.DESCRIPTION)
  validateSemver(descriptor.specVersion, ActionSpecVersion.NAME, ActionSpecVersion.DESCRIPTION)

  validateExists(descriptor.name, ActionName.NAME, ActionName.DESCRIPTION)
  validateNotEmptyIfExists(descriptor.name, ActionName.NAME, ActionName.DESCRIPTION)
  validateMaxLength(descriptor.name, ActionName.NAME, ActionName.DESCRIPTION, ActionName.MAX_LENGTH)
  validateMatchesRegexIfExistsAndNotEmpty(descriptor.name, ActionName.nameRegex, ActionName.NAME, ActionName.DESCRIPTION,
    "should only contain latin letters, numbers, dashes and underscores. " +
            "The property cannot start or end with a dash or underscore, and cannot contain several consecutive dashes and underscores.")

  validateExistsAndNotEmpty(descriptor.version, ActionVersion.NAME, ActionVersion.DESCRIPTION)
  validateSemver(descriptor.version, ActionVersion.NAME, ActionVersion.DESCRIPTION)

  validateExistsAndNotEmpty(descriptor.description, ActionDescription.NAME, ActionDescription.DESCRIPTION)
  validateMaxLength(
    descriptor.description,
    ActionDescription.NAME,
    ActionDescription.DESCRIPTION,
    ActionDescription.MAX_LENGTH,
  )

  validateNotEmptyIfExists(descriptor.steps, ActionSteps.NAME, ActionSteps.DESCRIPTION)
  for (input in descriptor.inputs) validateActionInput(input)
  for (requirement in descriptor.requirements) validateActionRequirement(requirement)
  for (step in descriptor.steps) validateActionStep(step)
}.toList()

private suspend fun SequenceScope<PluginProblem>.validateActionInput(input: Map<String, ActionInputDescriptor>) {
  if (input.size != 1) {
    yield(InvalidPropertyValueProblem("Wrong action input format. The input should consist of a name and body."))
    return
  }

  val inputName = input.keys.first()
  validateMaxLength(inputName, ActionInputName.NAME, ActionInputName.DESCRIPTION, ActionInputName.MAX_LENGTH)

  val value = input.values.first()

  validateExistsAndNotEmpty(value.type, ActionInputType.NAME, ActionInputType.DESCRIPTION)
  if (value.type != null && !enumContains<ActionInputTypeDescriptor>(value.type)) {
    yield(
      InvalidPropertyValueProblem(
        "Wrong action input type: ${value.type}. Supported values are: ${
          ActionInputTypeDescriptor.values().joinToString()
        }"
      )
    )
  }

  validateBooleanIfExists(value.isRequired, ActionInputRequired.NAME, ActionInputRequired.DESCRIPTION)

  validateNotEmptyIfExists(value.label, ActionInputLabel.NAME, ActionInputLabel.DESCRIPTION)
  validateMaxLength(value.label, ActionInputLabel.NAME, ActionInputLabel.DESCRIPTION, ActionInputLabel.MAX_LENGTH)

  validateNotEmptyIfExists(value.description, ActionInputDescription.NAME, ActionInputDescription.DESCRIPTION)
  validateMaxLength(
    value.description,
    ActionInputDescription.NAME,
    ActionInputDescription.DESCRIPTION,
    ActionInputDescription.MAX_LENGTH,
  )

  validateNotEmptyIfExists(value.defaultValue, ActionInputDefault.NAME, ActionInputDefault.DESCRIPTION)
  when (value.type) {
    ActionInputTypeDescriptor.boolean.name -> validateBooleanIfExists(
      value.defaultValue,
      ActionInputDefault.NAME,
      ActionInputDefault.DESCRIPTION,
    )

    ActionInputTypeDescriptor.select.name -> validateNotEmptyIfExists(
      value.selectOptions,
      ActionInputOptions.NAME,
      ActionInputOptions.DESCRIPTION,
    )
  }
}

private suspend fun SequenceScope<PluginProblem>.validateActionRequirement(requirement: Map<String, ActionRequirementDescriptor>) {
  if (requirement.size != 1) {
    yield(InvalidPropertyValueProblem("Wrong action requirement format. The requirement should consist of a name and body."))
    return
  }

  val requirementName = requirement.keys.first()
  validateMaxLength(
    requirementName,
    ActionRequirementName.NAME,
    ActionRequirementName.DESCRIPTION,
    ActionRequirementName.MAX_LENGTH,
  )

  val value = requirement.values.first()

  validateExistsAndNotEmpty(
    value.type,
    TeamCityActionSpec.ActionRequirementType.NAME,
    TeamCityActionSpec.ActionRequirementType.DESCRIPTION,
  )
  if (value.type == null) {
    return
  }
  val type: ActionRequirementType
  try {
    type = ActionRequirementType.from(value.type)
  } catch (e: IllegalArgumentException) {
    yield(
      InvalidPropertyValueProblem(
        "Wrong action requirement type '${value.type}'. " +
                "Supported values are: ${ActionRequirementType.values().joinToString { it.type }}"
      )
    )
    return
  }
  val description = "the value for ${value.type} requirement"
  if (type.isValueRequired && value.value == null) {
    yield(MissingValueProblem(ActionRequirementValue.NAME, description))
  } else if (!type.valueCanBeEmpty && value.value.isNullOrEmpty()) {
    yield(EmptyValueProblem(ActionRequirementValue.NAME, description))
  }
}

private suspend fun SequenceScope<PluginProblem>.validateActionStep(step: ActionStepDescriptor) {
  validateExistsAndNotEmpty(step.name, ActionStepName.NAME, ActionStepName.DESCRIPTION)
  validateMaxLength(step.name, ActionStepName.NAME, ActionStepName.DESCRIPTION, ActionStepName.MAX_LENGTH)

  if (step.with != null && step.script != null) {
    yield(
      PropertiesCombinationProblem(
        "The properties " +
                "<${ActionStepWith.NAME}> (${ActionStepWith.DESCRIPTION}) and " +
                "<${ActionStepScript.NAME}> (${ActionStepScript.DESCRIPTION}) " +
                "cannot be specified together for action step."
      )
    )
  } else if (step.with == null && step.script == null) {
    yield(
      PropertiesCombinationProblem(
        "One of the properties " +
                "<${ActionStepWith.NAME}> (${ActionStepWith.DESCRIPTION}) or " +
                "<${ActionStepScript.NAME}> (${ActionStepScript.DESCRIPTION}) should be specified for action step."
      )
    )
  } else if (step.with != null) {
    validateMaxLength(step.with, ActionStepWith.NAME, ActionStepWith.DESCRIPTION, ActionStepWith.MAX_LENGTH)
    if (ActionStepWith.allowedPrefixes.none { step.with.startsWith(it) }) {
      yield(
        InvalidPropertyValueProblem(
          "The property <${ActionStepWith.NAME}> (${ActionStepWith.DESCRIPTION}) should have " +
                  "a value starting with one of the following prefixes: ${ActionStepWith.allowedPrefixes.joinToString()}"
        )
      )
    }
  } else {
    validateNotEmptyIfExists(step.script, ActionStepScript.NAME, ActionStepScript.DESCRIPTION)
    validateMaxLength(
      step.script,
      ActionStepScript.NAME,
      ActionStepScript.DESCRIPTION,
      ActionStepScript.MAX_LENGTH,
    )
  }
}

private suspend fun SequenceScope<PluginProblem>.validateExists(
  propertyValue: String?,
  propertyName: String,
  propertyDescription: String,
) {
  if (propertyValue == null) {
    yield(MissingValueProblem(propertyName, propertyDescription))
  }
}

private suspend fun SequenceScope<PluginProblem>.validateNotEmptyIfExists(
  propertyValue: String?,
  propertyName: String,
  propertyDescription: String,
) {
  if (propertyValue != null && propertyValue.isEmpty()) {
    yield(EmptyValueProblem(propertyName, propertyDescription))
  }
}

private suspend fun <T> SequenceScope<PluginProblem>.validateNotEmptyIfExists(
  propertyValue: Iterable<T>,
  propertyName: String,
  propertyDescription: String,
) {
  if (!propertyValue.iterator().hasNext()) {
    yield(EmptyCollectionProblem(propertyName, propertyDescription))
  }
}

private suspend fun SequenceScope<PluginProblem>.validateExistsAndNotEmpty(
  propertyValue: String?,
  propertyName: String,
  propertyDescription: String,
) {
  validateExists(propertyValue, propertyName, propertyDescription)
  if (propertyValue != null) {
    validateNotEmptyIfExists(propertyValue, propertyName, propertyDescription)
  }
}

private suspend fun SequenceScope<PluginProblem>.validateMaxLength(
  propertyValue: String?,
  propertyName: String,
  propertyDescription: String,
  maxAllowedLength: Int,
) {
  if (propertyValue != null && propertyValue.length > maxAllowedLength) {
    yield(TooLongValueProblem(propertyName, propertyDescription, propertyValue.length, maxAllowedLength))
  }
}

private suspend fun SequenceScope<PluginProblem>.validateSemver(
  version: String?,
  propertyName: String,
  propertyDescription: String,
) {
  if (version != null) {
    try {
      Semver(version, Semver.SemverType.STRICT)
    } catch (e: SemverException) {
      yield(InvalidVersionProblem(propertyName, propertyDescription))
    }
  }
}

private suspend fun SequenceScope<PluginProblem>.validateBooleanIfExists(
  propertyValue: String?,
  propertyName: String,
  propertyDescription: String,
) {
  if (propertyValue != null && propertyValue != "true" && propertyValue != "false") {
    yield(InvalidBooleanProblem(propertyName, propertyDescription))
  }
}

private suspend fun SequenceScope<PluginProblem>.validateMatchesRegexIfExistsAndNotEmpty(
  propertyValue: String?,
  regex: Regex,
  propertyName: String,
  propertyDescription: String,
  validationFailureMessage: String,
) {
  if (!propertyValue.isNullOrEmpty() && !regex.matches(propertyValue)) {
    yield(InvalidPropertyValueProblem("The property <$propertyName> ($propertyDescription) $validationFailureMessage"))
  }
}

private inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
  return T::class.java.enumConstants.any { it.name == name }
}