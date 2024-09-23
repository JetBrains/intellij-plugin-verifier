package com.jetbrains.plugin.structure.teamcity.action

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionDescription
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputDefault
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputDescription
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputLabel
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputOptions
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputRequired
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputType
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionInputs
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionCompositeName
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionRequirementType
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionRequirementValue
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionRequirements
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionSpecVersion
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionStepName
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionStepParams
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionStepScript
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionStepWith
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionSteps
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionVersion

object Actions {
  val someAction = TeamCityActionBuilder(
    specVersion = "1.0.0",
    name = "action_namespace/action_name",
    version = "1.2.3",
    description = "some description",
    steps = listOf(
      ActionStepBuilder(
        stepName = "step name",
        with = "runner/someRunnerName",
      )
    ),
  )
}

object Inputs {
  val someActionTextInput = ActionInputBuilder(
    type = "text",
  )

  val someBooleanTextInput = ActionInputBuilder(
    type = "boolean",
  )

  val someSelectTextInput = ActionInputBuilder(
    type = "select",
    selectOptions = listOf("option 1", "option 2"),
  )
}

object Requirements {
  val someExistsRequirement = ActionRequirementBuilder(
    type = "exists",
  )
}

object Steps {
  val someWithStep = ActionStepBuilder(
    stepName = "some step",
    with = "runner/some_runner"
  )

  val someScriptStep = ActionStepBuilder(
    stepName = "some step",
    script = "echo \"hello world\""
  )
}

data class TeamCityActionBuilder(
  @JsonProperty(ActionSpecVersion.NAME)
  var specVersion: String? = null,
  @JsonProperty(ActionCompositeName.NAME)
  var name: String? = null,
  @JsonProperty(ActionVersion.NAME)
  var version: String? = null,
  @JsonProperty(ActionDescription.NAME)
  var description: String? = null,
  @JsonProperty(ActionInputs.NAME)
  var inputs: List<Map<String, ActionInputBuilder>> = emptyList(),
  @JsonProperty(ActionRequirements.NAME)
  var requirements: List<Map<String, ActionRequirementBuilder>> = emptyList(),
  @JsonProperty(ActionSteps.NAME)
  var steps: List<ActionStepBuilder> = emptyList(),
)

data class ActionInputBuilder(
  @JsonProperty(ActionInputType.NAME)
  var type: String? = null,
  @JsonProperty(ActionInputRequired.NAME)
  var required: String? = null,
  @JsonProperty(ActionInputLabel.NAME)
  var label: String? = null,
  @JsonProperty(ActionInputDescription.NAME)
  var description: String? = null,
  @JsonProperty(ActionInputDefault.NAME)
  var defaultValue: String? = null,
  @JsonProperty(ActionInputOptions.NAME)
  var selectOptions: List<String> = emptyList(),
)

data class ActionRequirementBuilder(
  @JsonProperty(ActionRequirementType.NAME)
  var type: String? = null,
  @JsonProperty(ActionRequirementValue.NAME)
  var value: String? = null,
)

data class ActionStepBuilder(
  @JsonProperty(ActionStepName.NAME)
  var stepName: String? = null,
  @JsonProperty(ActionStepWith.NAME)
  var with: String? = null,
  @JsonProperty(ActionStepScript.NAME)
  var script: String? = null,
  @JsonProperty(ActionStepParams.NAME)
  var params: Map<String, String> = emptyMap(),
)

fun randomAlphanumeric(len: Int): String {
  val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
  return (1..len)
    .map { allowedChars.random() }
    .joinToString("")
}