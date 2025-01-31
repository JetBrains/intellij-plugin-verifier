package com.jetbrains.plugin.structure.teamcity.recipe

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeCompositeName
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeContainerImage
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeContainerRunParameters
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeDescription
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputDefault
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputDescription
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputLabel
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputName
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputOptions
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputRequired
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputType
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepCommandLineScript
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepKotlinScript
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepName
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepReference
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeSteps
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeVersion

internal fun validateTeamCityRecipe(descriptor: TeamCityRecipeDescriptor) = sequence {
  validateName(descriptor.name)

  validateExistsAndNotEmpty(descriptor.version, RecipeVersion.NAME, RecipeVersion.DESCRIPTION)
  validateVersion(descriptor.version)

  validateExistsAndNotEmpty(descriptor.description, RecipeDescription.NAME, RecipeDescription.DESCRIPTION)
  validateMaxLength(
    descriptor.description,
    RecipeDescription.NAME,
    RecipeDescription.DESCRIPTION,
    RecipeDescription.MAX_LENGTH,
  )

  validateContainer(descriptor.container)
  validateNotEmptyIfExists(descriptor.steps, RecipeSteps.NAME, RecipeSteps.DESCRIPTION)
  for (input in descriptor.inputs) validateRecipeInput(input)
  for (step in descriptor.steps) validateRecipeStep(step)
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
      RecipeCompositeName.NameInNamespace.NAME,
      RecipeCompositeName.NameInNamespace.DESCRIPTION,
      RecipeCompositeName.NameInNamespace.MIN_LENGTH,
    )
    validateMaxLength(
      nameInNamespace,
      RecipeCompositeName.NameInNamespace.NAME,
      RecipeCompositeName.NameInNamespace.DESCRIPTION,
      RecipeCompositeName.NameInNamespace.MAX_LENGTH,
    )
    validateMatchesRegexIfExistsAndNotEmpty(
      nameInNamespace,
      RecipeCompositeName.meaningfulPartRegex,
      RecipeCompositeName.NameInNamespace.NAME,
      RecipeCompositeName.NameInNamespace.DESCRIPTION,
      "should only contain latin letters, numbers, dashes and underscores. " +
          "The property cannot start or end with a dash or underscore, and cannot contain several consecutive dashes and underscores.",
    )
  }
}

private suspend fun SequenceScope<PluginProblem>.validateRecipeInput(input: Map<String, RecipeInputDescriptor>) {
  if (input.size != 1) {
    yield(InvalidPropertyValueProblem("Wrong recipe input format. The input should consist of a name and a body."))
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

  validateNotEmptyIfExists(value.defaultValue, RecipeInputDefault.NAME, RecipeInputDefault.DESCRIPTION, allowWhitespaceValues = true)
  when (value.type) {
    RecipeInputTypeDescriptor.boolean.name -> validateBooleanIfExists(
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

private suspend fun SequenceScope<PluginProblem>.validateRecipeStep(step: RecipeStepDescriptor) {
  validateExistsAndNotEmpty(step.name, RecipeStepName.NAME, RecipeStepName.DESCRIPTION)
  validateMaxLength(step.name, RecipeStepName.NAME, RecipeStepName.DESCRIPTION, RecipeStepName.MAX_LENGTH)
  validateContainer(step.container)

  val stepContentProperties = listOfNotNull(step.uses, step.commandLineScript, step.kotlinScript)
  if (stepContentProperties.size > 1) {
    yield(
      PropertiesCombinationProblem(
        "The properties " +
                "<${RecipeStepCommandLineScript.NAME}> (${RecipeStepCommandLineScript.DESCRIPTION}), " +
                "<${RecipeStepKotlinScript.NAME}> (${RecipeStepKotlinScript.DESCRIPTION}) and " +
                "<${RecipeStepReference.NAME}> (${RecipeStepReference.DESCRIPTION}) cannot be specified together in a recipe step."
      )
    )
  } else if (step.uses == null && step.commandLineScript == null && step.kotlinScript == null) {
    yield(
      PropertiesCombinationProblem(
        "Either <${RecipeStepCommandLineScript.NAME}> (${RecipeStepCommandLineScript.DESCRIPTION}), " +
                "<${RecipeStepKotlinScript.NAME}> (${RecipeStepKotlinScript.DESCRIPTION}) or " +
                "<${RecipeStepReference.NAME}> (${RecipeStepReference.DESCRIPTION}) should be specified in a recipe step."
      )
    )
  }

  if (step.uses != null) {
    validateStepReference(step.uses)
  } else if (step.commandLineScript != null) {
    validateNotEmptyIfExists(step.commandLineScript, RecipeStepCommandLineScript.NAME, RecipeStepCommandLineScript.DESCRIPTION)
    validateMaxLength(
      step.commandLineScript,
      RecipeStepCommandLineScript.NAME,
      RecipeStepCommandLineScript.DESCRIPTION,
      RecipeStepCommandLineScript.MAX_LENGTH,
    )
  } else if (step.kotlinScript != null) {
    validateNotEmptyIfExists(step.kotlinScript, RecipeStepKotlinScript.NAME, RecipeStepKotlinScript.DESCRIPTION)
    validateMaxLength(
      step.kotlinScript,
      RecipeStepKotlinScript.NAME,
      RecipeStepKotlinScript.DESCRIPTION,
      RecipeStepKotlinScript.MAX_LENGTH,
    )
  }
}

private suspend fun SequenceScope<PluginProblem>.validateStepReference(reference: String) {
  validateMaxLength(reference, RecipeStepReference.NAME, RecipeStepReference.DESCRIPTION, RecipeStepReference.MAX_LENGTH)

  // TODO
  val recipeIdParts = reference.split("@")
  if (recipeIdParts.size != 2) {
    yield(
      InvalidPropertyValueProblem(
          "The property <${RecipeStepReference.NAME}> (${RecipeStepReference.DESCRIPTION}) has an invalid recipe reference: $reference. " +
                  "The reference must follow the '<namespace>/<name>@<version>' format."
      )
    )
  } else {
    val compositeName = recipeIdParts[0]
    if (!RecipeCompositeName.compositeNameRegex.matches(compositeName)) {
      yield(
        InvalidPropertyValueProblem(
          "The property <${RecipeStepReference.NAME}> (${RecipeStepReference.DESCRIPTION}) has an invalid recipe reference: $reference. " +
                  "The reference must have a valid composite name in the '<namespace>/<name>' format."
        )
      )
    }

    val version = recipeIdParts[1]
    if (!isValidRecipeVersion(version)) {
      yield(
        InvalidPropertyValueProblem(
          "The property <${RecipeStepReference.NAME}> (${RecipeStepReference.DESCRIPTION}) has an invalid recipe reference: $reference. " +
                  "The reference must have a valid recipe version in the '<major>.<minor>.<patch>' format."
        )
      )
    }
  }
}

private suspend fun SequenceScope<PluginProblem>.validateContainer(container: RecipeContainerDescriptor?) {
  if (container == null) return

  validateExistsAndNotEmpty(
    container.image,
    RecipeContainerImage.NAME,
    RecipeContainerImage.DESCRIPTION
  )

  validateNotEmptyIfExists(
    container.runParameters,
    RecipeContainerRunParameters.NAME,
    RecipeContainerRunParameters.DESCRIPTION
  )

  if (container.imagePlatform != null && !enumContains<RecipeContainerImagePlatformDescriptor>(container.imagePlatform, ignoreCase = true)) {
    yield(
      InvalidPropertyValueProblem(
        "Wrong recipe container image platform: ${container.imagePlatform}. Supported values are: ${RecipeContainerImagePlatformDescriptor.values().joinToString()}."
      )
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
  allowWhitespaceValues: Boolean = false,
) {
  if (propertyValue == null) return
  if ((!allowWhitespaceValues && propertyValue.isBlank()) || (allowWhitespaceValues && propertyValue.isEmpty())) {
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

private suspend fun SequenceScope<PluginProblem>.validateVersion(version: String?) {
  if (version == null) return

  if (!isValidRecipeVersion(version)) {
    yield(
      InvalidPropertyValueProblem(
        "The property <${RecipeVersion.NAME}> (${RecipeVersion.DESCRIPTION}) has an invalid value. " +
                "The version must be in the '<major>.<minor>.<patch>' format."
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

private fun isValidRecipeVersion(version: String) : Boolean {
  val versionParts = version.split(".")
  if (versionParts.size != 3) return false

  versionParts.forEach {
    val intValue = it.toIntOrNull()
    if (intValue == null || intValue < 0) return false
  }

  return true
}

private inline fun <reified T : Enum<T>> enumContains(name: String, ignoreCase: Boolean = false): Boolean {
  return T::class.java.enumConstants.any { it.name.equals(name, ignoreCase) }
}