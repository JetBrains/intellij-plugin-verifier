package com.jetbrains.plugin.structure.teamcity.recipe

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeCompositeName
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeContainer
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeContainerImage
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeContainerImagePlatform
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeContainerRunParameters
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeDescription
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputDefault
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputDescription
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputLabel
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputOptions
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputRequired
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputType
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputs
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepCommandLineScript
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepKotlinScript
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepName
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepParams
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepReference
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeSteps
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeVersion

data class TeamCityRecipeDescriptor(
  @JsonProperty(RecipeCompositeName.NAME)
  val name: String? = null,
  @JsonProperty(RecipeVersion.NAME)
  val version: String? = null,
  @JsonProperty(RecipeDescription.NAME)
  val description: String? = null,
  @JsonProperty(RecipeContainer.NAME)
  @JsonDeserialize(using = RecipeContainerDeserializer::class)
  val container: RecipeContainerDescriptor? = null,
  @JsonProperty(RecipeInputs.NAME)
  val inputs: List<Map<String, RecipeInputDescriptor>> = emptyList(),
  @JsonProperty(RecipeSteps.NAME)
  val steps: List<RecipeStepDescriptor> = emptyList(),
)

data class RecipeInputDescriptor(
  @JsonProperty(RecipeInputType.NAME)
  val type: String? = null,
  @JsonProperty(RecipeInputRequired.NAME)
  val required: String? = null,
  @JsonProperty(RecipeInputLabel.NAME)
  val label: String? = null,
  @JsonProperty(RecipeInputDescription.NAME)
  val description: String? = null,
  @JsonProperty(RecipeInputDefault.NAME)
  val defaultValue: String? = null,
  @JsonProperty(RecipeInputOptions.NAME)
  val selectOptions: List<String> = emptyList(),
)

@Suppress("EnumEntryName")
enum class RecipeInputTypeDescriptor {
  text,
  boolean,
  select,
  password,
}

data class RecipeStepDescriptor(
  @JsonProperty(RecipeStepName.NAME)
  val name: String? = null,
  @JsonProperty(RecipeContainer.NAME)
  @JsonDeserialize(using = RecipeContainerDeserializer::class)
  val container: RecipeContainerDescriptor? = null,
  @JsonProperty(RecipeStepReference.NAME)
  val uses: String? = null,
  @JsonProperty(RecipeStepCommandLineScript.NAME)
  val commandLineScript: String? = null,
  @JsonProperty(RecipeStepKotlinScript.NAME)
  val kotlinScript: String? = null,
  @JsonProperty(RecipeStepParams.NAME)
  val parameters: Map<String, String> = emptyMap(),
)

data class RecipeContainerDescriptor(
  @JsonProperty(RecipeContainerImage.NAME)
  val image: String? = null,
  @JsonProperty(RecipeContainerImagePlatform.NAME)
  val imagePlatform: String? = null,
  @JsonProperty(RecipeContainerRunParameters.NAME)
  val runParameters: String? = null
)

enum class RecipeContainerImagePlatformDescriptor {
  Linux,
  Windows,
}