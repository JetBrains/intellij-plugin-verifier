package com.jetbrains.plugin.structure.teamcity.action

import com.jetbrains.plugin.structure.base.problems.InvalidSemverFormat
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.SemverComponentLimitExceeded
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionDescription
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputDefault
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputDescription
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputLabel
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputName
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputOptions
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputRequired
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputType
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionCompositeName
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionRequirementName
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionRequirementValue
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionSpecVersion
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionStepName
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionStepScript
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionStepWith
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionSteps
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionVersion
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.SemverException

internal fun validateTeamCityAction(descriptor: TeamCityActionDescriptor) = sequence {
  validateName(descriptor.name)

  validateSpecVersion(descriptor.specVersion, ActionSpecVersion.NAME, ActionSpecVersion.DESCRIPTION)

  validateExistsAndNotEmpty(descriptor.version, ActionVersion.NAME, ActionVersion.DESCRIPTION)
  validateSemver(descriptor.version, ActionVersion.NAME)

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

private suspend fun SequenceScope<PluginProblem>.validateName(name: String?) {
  validateExists(name, ActionCompositeName.NAME, ActionCompositeName.DESCRIPTION)
  validateNotEmptyIfExists(name, ActionCompositeName.NAME, ActionCompositeName.DESCRIPTION)
  validateMatchesRegexIfExistsAndNotEmpty(
    name, ActionCompositeName.compositeNameRegex, ActionCompositeName.NAME, ActionCompositeName.DESCRIPTION,
    "should consist of namespace and name parts. Both parts should only contain latin letters, numbers, dashes and underscores."
  )

  val namespace = ActionCompositeName.getNamespace(name)
  if (namespace != null) {
    validateMinLength(namespace, ActionCompositeName.Namespace.NAME, ActionCompositeName.Namespace.DESCRIPTION, ActionCompositeName.Namespace.MIN_LENGTH)
    validateMaxLength(namespace, ActionCompositeName.Namespace.NAME, ActionCompositeName.Namespace.DESCRIPTION, ActionCompositeName.Namespace.MAX_LENGTH)
    validateMatchesRegexIfExistsAndNotEmpty(
      namespace, ActionCompositeName.idAndNamespaceRegex, ActionCompositeName.Namespace.NAME, ActionCompositeName.Namespace.DESCRIPTION,
      "should only contain latin letters, numbers, dashes and underscores. " +
              "The property cannot start or end with a dash or underscore, and cannot contain several consecutive dashes and underscores."
    )
  }

  val nameInNamespace = ActionCompositeName.getNameInNamespace(name)
  if (nameInNamespace != null) {
    validateMinLength(nameInNamespace, ActionCompositeName.Name.NAME, ActionCompositeName.Name.DESCRIPTION, ActionCompositeName.Name.MIN_LENGTH)
    validateMaxLength(nameInNamespace, ActionCompositeName.Name.NAME, ActionCompositeName.Name.DESCRIPTION, ActionCompositeName.Name.MAX_LENGTH)
    validateMatchesRegexIfExistsAndNotEmpty(
      nameInNamespace, ActionCompositeName.idAndNamespaceRegex, ActionCompositeName.Name.NAME, ActionCompositeName.Name.DESCRIPTION,
      "should only contain latin letters, numbers, dashes and underscores. " +
              "The property cannot start or end with a dash or underscore, and cannot contain several consecutive dashes and underscores."
    )
  }
}

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

private suspend fun SequenceScope<PluginProblem>.validateMinLength(
  propertyValue: String?,
  propertyName: String,
  propertyDescription: String,
  minAllowedLength: Int,
) {
  if (propertyValue != null && propertyValue.length < minAllowedLength) {
    yield(TooShortValueProblem(propertyName, propertyDescription, propertyValue.length, minAllowedLength))
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
  propertyName: String
): Semver? {
  if (version != null) {
    try {
      return TeamCityActionSpecVersionUtils.getSemverFromString(version)
    } catch (e: SemverException) {
      yield(InvalidSemverFormat(
        versionName = propertyName,
        version = version
      ))
    }
  }
  return null
}

private suspend fun SequenceScope<PluginProblem>.validateSpecVersion(
  version: String?,
  propertyName: String,
  propertyDescription: String
) {
  validateExistsAndNotEmpty(version, propertyName, propertyDescription)
  val semver = validateSemver(version, propertyName) ?: return

  validateSemverComponentLimits(
    component = semver.major,
    componentName = "major",
    limit = TeamCityActionSpecVersionUtils.MAX_MAJOR_VALUE,
    propertyName = propertyName,
    propertyValue = semver.originalValue
  )
  validateSemverComponentLimits(
    component = semver.minor,
    componentName = "minor",
    limit = TeamCityActionSpecVersionUtils.VERSION_MINOR_LENGTH,
    propertyName = propertyName,
    propertyValue = semver.originalValue
  )
  validateSemverComponentLimits(
    component = semver.patch,
    componentName = "patch",
    limit = TeamCityActionSpecVersionUtils.VERSION_PATCH_LENGTH,
    propertyName = propertyName,
    propertyValue = semver.originalValue
  )
}

private suspend fun SequenceScope<PluginProblem>.validateSemverComponentLimits(
  component: Int?,
  componentName: String,
  limit: Int,
  propertyName: String,
  propertyValue: String
) {
  if (component == null) return

  if (component >= limit) {
    yield(
      SemverComponentLimitExceeded(
        componentName = componentName,
        versionName = propertyName,
        version = propertyValue,
        limit = limit - 1
      )
    )
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