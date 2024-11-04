package com.jetbrains.plugin.structure.teamcity.recipe

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeCompositeName
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeDescription
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputDefault
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputDescription
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputLabel
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputOptions
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputRequired
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputType
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeInputs
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeRequirementType
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeRequirementValue
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeRequirements
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepName
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepParams
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepScript
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeStepWith
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeSteps
import com.jetbrains.plugin.structure.teamcity.recipe.TeamCityRecipeSpec.RecipeVersion

data class TeamCityRecipeDescriptor(
  @JsonProperty(RecipeCompositeName.NAME)
  val name: String? = null,
  @JsonProperty(RecipeVersion.NAME)
  val version: String? = null,
  @JsonProperty(RecipeDescription.NAME)
  val description: String? = null,
  @JsonProperty(RecipeInputs.NAME)
  val inputs: List<Map<String, RecipeInputDescriptor>> = emptyList(),
  @JsonProperty(RecipeRequirements.NAME)
  val requirements: List<Map<String, RecipeRequirementDescriptor>> = emptyList(),
  @JsonProperty(RecipeSteps.NAME)
  val steps: List<RecipeStepDescriptor> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
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
  number,
  select,
  password,
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RecipeRequirementDescriptor(
  @JsonProperty(RecipeRequirementType.NAME)
  val type: String? = null,
  @JsonProperty(RecipeRequirementValue.NAME)
  val value: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RecipeStepDescriptor(
  @JsonProperty(RecipeStepName.NAME)
  val name: String? = null,
  @JsonProperty(RecipeStepWith.NAME)
  val with: String? = null,
  @JsonProperty(RecipeStepScript.NAME)
  val script: String? = null,
  @JsonProperty(RecipeStepParams.NAME)
  val parameters: Map<String, String> = emptyMap(),
)