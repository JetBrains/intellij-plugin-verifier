package com.jetbrains.plugin.structure.teamcity.recipe

import com.jetbrains.plugin.structure.base.problems.InvalidSemverFormat
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeCompositeName
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeDescription
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputDefault
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputDescription
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputLabel
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputName
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputOptions
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputRequired
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputType
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeRequirementName
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeRequirementValue
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepName
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepScript
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepWith
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepWith.RECIPE_PREFIX
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepWith.RUNNER_PREFIX
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeSteps
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeVersion
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.SemverException

internal fun validateTeamCityRecipe(recipe: TeamCityRecipeDescriptor) = sequence {
  validateName(recipe.name)

  validateExistsAndNotEmpty(recipe.version, RecipeVersion.NAME, RecipeVersion.DESCRIPTION)
  validateSemver(recipe.version, RecipeVersion.NAME)

  validateExistsAndNotEmpty(recipe.description, RecipeDescription.NAME, RecipeDescription.DESCRIPTION)
  validateMaxLength(
    recipe.description,
    RecipeDescription.NAME,
    RecipeDescription.DESCRIPTION,
    RecipeDescription.MAX_LENGTH,
  )

  validateNotEmptyIfExists(recipe.steps, RecipeSteps.NAME, RecipeSteps.DESCRIPTION)
  for (input in recipe.inputs) validateRecipeInput(input)
  for (requirement in recipe.requirements) validateRecipeRequirement(requirement)
  for (step in recipe.steps) validateRecipeStep(step)
}.toList()

private suspend fun SequenceScope<PluginProblem>.validateName(name: String?) {
  validateExists(name, RecipeCompositeName.NAME, RecipeCompositeName.DESCRIPTION)
  validateNotEmptyIfExists(name, RecipeCompositeName.NAME, RecipeCompositeName.DESCRIPTION)
  validateMatchesRegexIfExistsAndNotEmpty(
    name, RecipeCompositeName.compositeNameRegex, RecipeCompositeName.NAME, RecipeCompositeName.DESCRIPTION,
    "should consist of namespace and name parts. Both parts should only contain latin letters, numbers, dashes and underscores."
  )

  val namespace = RecipeCompositeName.getNamespace(name)
  if (namespace != null) {
    validateMinLength(
      namespace,
      RecipeCompositeName.Namespace.NAME,
      RecipeCompositeName.Namespace.DESCRIPTION,
      RecipeCompositeName.Namespace.MIN_LENGTH,
    )
    validateMaxLength(
      namespace,
      RecipeCompositeName.Namespace.NAME,
      RecipeCompositeName.Namespace.DESCRIPTION,
      RecipeCompositeName.Namespace.MAX_LENGTH,
    )
    validateMatchesRegexIfExistsAndNotEmpty(
      namespace,
      RecipeCompositeName.meaningfulPartRegex,
      RecipeCompositeName.Namespace.NAME,
      RecipeCompositeName.Namespace.DESCRIPTION,
      "should only contain latin letters, numbers, dashes and underscores. " +
          "The property cannot start or end with a dash or underscore, and cannot contain several consecutive dashes and underscores.",
    )
  }

  val nameInNamespace = RecipeCompositeName.getNameInNamespace(name)
  if (nameInNamespace != null) {
    validateMinLength(
      nameInNamespace,
      RecipeCompositeName.Name.NAME,
      RecipeCompositeName.Name.DESCRIPTION,
      RecipeCompositeName.Name.MIN_LENGTH,
    )
    validateMaxLength(
      nameInNamespace,
      RecipeCompositeName.Name.NAME,
      RecipeCompositeName.Name.DESCRIPTION,
      RecipeCompositeName.Name.MAX_LENGTH,
    )
    validateMatchesRegexIfExistsAndNotEmpty(
      nameInNamespace,
      RecipeCompositeName.meaningfulPartRegex,
      RecipeCompositeName.Name.NAME,
      RecipeCompositeName.Name.DESCRIPTION,
      "should only contain latin letters, numbers, dashes and underscores. " +
          "The property cannot start or end with a dash or underscore, and cannot contain several consecutive dashes and underscores.",
    )
  }
}

private suspend fun SequenceScope<PluginProblem>.validateRecipeInput(input: Map<String, RecipeInputDescriptor>) {
  if (input.size != 1) {
    yield(InvalidPropertyValueProblem("Wrong recipe input format. The input should consist of a name and body."))
    return
  }

  val inputName = input.keys.first()
  validateMaxLength(inputName, RecipeInputName.NAME, RecipeInputName.DESCRIPTION, RecipeInputName.MAX_LENGTH)

  val value = input.values.first()

  validateExistsAndNotEmpty(value.type, RecipeInputType.NAME, RecipeInputType.DESCRIPTION)
  if (value.type != null && !enumContains<RecipeInputTypeDescriptor>(value.type)) {
    yield(
      InvalidPropertyValueProblem(
        "Wrong recipe input type: ${value.type}. Supported values are: ${
          RecipeInputTypeDescriptor.values().joinToString()
        }"
      )
    )
  }

  validateBooleanIfExists(value.required, RecipeInputRequired.NAME, RecipeInputRequired.DESCRIPTION)

  validateNotEmptyIfExists(value.label, RecipeInputLabel.NAME, RecipeInputLabel.DESCRIPTION)
  validateMaxLength(value.label, RecipeInputLabel.NAME, RecipeInputLabel.DESCRIPTION, RecipeInputLabel.MAX_LENGTH)

  validateNotEmptyIfExists(value.description, RecipeInputDescription.NAME, RecipeInputDescription.DESCRIPTION)
  validateMaxLength(
    value.description,
    RecipeInputDescription.NAME,
    RecipeInputDescription.DESCRIPTION,
    RecipeInputDescription.MAX_LENGTH,
  )

  validateNotEmptyIfExists(value.defaultValue, RecipeInputDefault.NAME, RecipeInputDefault.DESCRIPTION)
  when (value.type) {
    RecipeInputTypeDescriptor.boolean.name -> validateBooleanIfExists(
      value.defaultValue,
      RecipeInputDefault.NAME,
      RecipeInputDefault.DESCRIPTION,
    )

    RecipeInputTypeDescriptor.number.name -> validateNumberIfExists(
      value.defaultValue,
      RecipeInputDefault.NAME,
      RecipeInputDefault.DESCRIPTION,
    )

    RecipeInputTypeDescriptor.select.name -> validateNotEmptyIfExists(
      value.selectOptions,
      RecipeInputOptions.NAME,
      RecipeInputOptions.DESCRIPTION,
    )
  }
}

private suspend fun SequenceScope<PluginProblem>.validateRecipeRequirement(requirement: Map<String, RecipeRequirementDescriptor>) {
  if (requirement.size != 1) {
    yield(InvalidPropertyValueProblem("Wrong recipe requirement format. The requirement should consist of a name and body."))
    return
  }

  val requirementName = requirement.keys.first()
  validateMaxLength(
    requirementName,
    RecipeRequirementName.NAME,
    RecipeRequirementName.DESCRIPTION,
    RecipeRequirementName.MAX_LENGTH,
  )

  val value = requirement.values.first()

  validateExistsAndNotEmpty(
    value.type,
    TeamCityRecipeSpec.RecipeRequirementType.NAME,
    TeamCityRecipeSpec.RecipeRequirementType.DESCRIPTION,
  )
  if (value.type == null) {
    return
  }
  val type: RecipeRequirementType
  try {
    type = RecipeRequirementType.from(value.type)
  } catch (e: IllegalArgumentException) {
    yield(
      InvalidPropertyValueProblem(
        "Wrong recipe requirement type '${value.type}'. " +
            "Supported values are: ${RecipeRequirementType.values().joinToString { it.type }}"
      )
    )
    return
  }
  val description = "the value for ${value.type} requirement"
  if (type.isValueRequired && value.value == null) {
    yield(MissingValueProblem(RecipeRequirementValue.NAME, description))
  } else if (!type.valueCanBeEmpty && value.value.isNullOrEmpty()) {
    yield(EmptyValueProblem(RecipeRequirementValue.NAME, description))
  }
}

private suspend fun SequenceScope<PluginProblem>.validateRecipeStep(step: RecipeStepDescriptor) {
  validateExistsAndNotEmpty(step.name, RecipeStepName.NAME, RecipeStepName.DESCRIPTION)
  validateMaxLength(step.name, RecipeStepName.NAME, RecipeStepName.DESCRIPTION, RecipeStepName.MAX_LENGTH)

  if (step.with != null && step.script != null) {
    yield(
      PropertiesCombinationProblem(
        "The properties " +
            "<${RecipeStepWith.NAME}> (${RecipeStepWith.DESCRIPTION}) and " +
            "<${RecipeStepScript.NAME}> (${RecipeStepScript.DESCRIPTION}) " +
            "cannot be specified together for recipe step."
      )
    )
  } else if (step.with == null && step.script == null) {
    yield(
      PropertiesCombinationProblem(
        "One of the properties " +
            "<${RecipeStepWith.NAME}> (${RecipeStepWith.DESCRIPTION}) or " +
            "<${RecipeStepScript.NAME}> (${RecipeStepScript.DESCRIPTION}) should be specified for recipe step."
      )
    )
  } else if (step.with != null) {
    validateStepReference(step)
  } else {
    validateNotEmptyIfExists(step.script, RecipeStepScript.NAME, RecipeStepScript.DESCRIPTION)
    validateMaxLength(
      step.script,
      RecipeStepScript.NAME,
      RecipeStepScript.DESCRIPTION,
      RecipeStepScript.MAX_LENGTH,
    )
  }
}

private suspend fun SequenceScope<PluginProblem>.validateStepReference(step: RecipeStepDescriptor) {
  validateMaxLength(step.with, RecipeStepWith.NAME, RecipeStepWith.DESCRIPTION, RecipeStepWith.MAX_LENGTH)
  when {
    step.with!!.startsWith(RUNNER_PREFIX) -> {
      val runnerName = step.with.substringAfter(RUNNER_PREFIX)
      val allowedParams = allowedRunnerToAllowedParams[runnerName]
      if (allowedParams == null) {
        yield(UnsupportedRunnerProblem(runnerName, allowedRunnerToAllowedParams.keys))
      } else {
        val unsupportedParams = step.parameters.filter { !allowedParams.contains(it.key) }.keys
        if (unsupportedParams.isNotEmpty()) {
          yield(UnsupportedRunnerParamsProblem(runnerName, unsupportedParams, allowedParams))
        }
      }
    }

    step.with.startsWith(RECIPE_PREFIX) -> {
      val recipeId = step.with.substringAfter(RECIPE_PREFIX)
      val recipeIdParts = recipeId.split("@")
      if (recipeIdParts.size != 2) {
        yield(
          InvalidPropertyValueProblem(
            "The property <${RecipeStepWith.NAME}> (${RecipeStepWith.DESCRIPTION}) has an invalid recipe reference: $recipeId. " +
                "The reference must follow '${RECIPE_PREFIX}name@version' format"
          )
        )
      }
    }

    else -> {
      yield(
        InvalidPropertyValueProblem(
          "The property <${RecipeStepWith.NAME}> (${RecipeStepWith.DESCRIPTION}) should be either a runner or an recipe reference. " +
              "The value should start with '$RUNNER_PREFIX' or '$RECIPE_PREFIX' prefix"
        )
      )
    }
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
  propertyName: String,
): Semver? {
  if (version != null) {
    try {
      return TeamCityRecipeSpecVersionUtils.getSemverFromString(version)
    } catch (e: SemverException) {
      yield(
        InvalidSemverFormat(
          versionName = propertyName,
          version = version,
        )
      )
    }
  }
  return null
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

private suspend fun SequenceScope<PluginProblem>.validateNumberIfExists(
  propertyValue: String?,
  propertyName: String,
  propertyDescription: String,
) {
  if (propertyValue != null && propertyValue.toLongOrNull() == null) {
    yield(InvalidNumberProblem(propertyName, propertyDescription))
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