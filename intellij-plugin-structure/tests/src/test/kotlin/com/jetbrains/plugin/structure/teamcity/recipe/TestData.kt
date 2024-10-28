package com.jetbrains.plugin.structure.teamcity.recipe

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jetbrains.plugin.structure.base.utils.writeBytes
import com.jetbrains.plugin.structure.rules.FileSystemAwareTemporaryFolder
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
import java.nio.file.Path

val ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

object Recipes {
  val someRecipe = TeamCityRecipeBuilder(
    name = "recipe_namespace/recipe_name",
    version = "1.2.3",
    description = "some description",
    steps = listOf(
      RecipeStepBuilder(
        stepName = "step name",
        with = "runner/command-line",
      )
    ),
  )
}

object Inputs {
  val someRecipeTextInput = RecipeInputBuilder(
    type = "text",
  )

  val someBooleanTextInput = RecipeInputBuilder(
    type = "boolean",
  )

  val someSelectTextInput = RecipeInputBuilder(
    type = "select",
    selectOptions = listOf("option 1", "option 2"),
  )

  val someNumberInput = RecipeInputBuilder(
    type = "number",
  )
}

object Requirements {
  val someExistsRequirement = RecipeRequirementBuilder(
    type = "exists",
  )
}

object Steps {
  val someWithStep = RecipeStepBuilder(
    stepName = "some step",
    with = "runner/command-line"
  )

  val someScriptStep = RecipeStepBuilder(
    stepName = "some step",
    script = "echo \"hello world\""
  )
}

data class TeamCityRecipeBuilder(
  @JsonProperty(RecipeCompositeName.NAME)
  var name: String? = null,
  @JsonProperty(RecipeVersion.NAME)
  var version: String? = null,
  @JsonProperty(RecipeDescription.NAME)
  var description: String? = null,
  @JsonProperty(RecipeInputs.NAME)
  var inputs: List<Map<String, RecipeInputBuilder>> = emptyList(),
  @JsonProperty(RecipeRequirements.NAME)
  var requirements: List<Map<String, RecipeRequirementBuilder>> = emptyList(),
  @JsonProperty(RecipeSteps.NAME)
  var steps: List<RecipeStepBuilder> = emptyList(),
)

data class RecipeInputBuilder(
  @JsonProperty(RecipeInputType.NAME)
  var type: String? = null,
  @JsonProperty(RecipeInputRequired.NAME)
  var required: String? = null,
  @JsonProperty(RecipeInputLabel.NAME)
  var label: String? = null,
  @JsonProperty(RecipeInputDescription.NAME)
  var description: String? = null,
  @JsonProperty(RecipeInputDefault.NAME)
  var defaultValue: String? = null,
  @JsonProperty(RecipeInputOptions.NAME)
  var selectOptions: List<String> = emptyList(),
)

data class RecipeRequirementBuilder(
  @JsonProperty(RecipeRequirementType.NAME)
  var type: String? = null,
  @JsonProperty(RecipeRequirementValue.NAME)
  var value: String? = null,
)

data class RecipeStepBuilder(
  @JsonProperty(RecipeStepName.NAME)
  var stepName: String? = null,
  @JsonProperty(RecipeStepWith.NAME)
  var with: String? = null,
  @JsonProperty(RecipeStepScript.NAME)
  var script: String? = null,
  @JsonProperty(RecipeStepParams.NAME)
  var params: Map<String, String> = emptyMap(),
)

fun randomAlphanumeric(len: Int): String {
  val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
  return (1..len)
    .map { allowedChars.random() }
    .joinToString("")
}

fun FileSystemAwareTemporaryFolder.prepareRecipeYaml(builder: TeamCityRecipeBuilder): Path {
  return prepareRecipeYaml(ObjectMapper.writeValueAsString(builder))
}

fun FileSystemAwareTemporaryFolder.prepareRecipeYaml(recipeYaml: String): Path {
  val directory = this.newFolder("plugin")
  val file = directory.resolve("recipe.yaml")
  file.writeBytes(recipeYaml.toByteArray())
  return file
}