package com.jetbrains.plugin.structure.teamcity.action

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class TeamCityActionDescriptor(
  @JsonProperty(ActionSpecVersion.NAME)
  val specVersion: String? = null,
  @JsonProperty(ActionCompositeName.NAME)
  val name: String? = null,
  @JsonProperty(ActionVersion.NAME)
  val version: String? = null,
  @JsonProperty(ActionDescription.NAME)
  val description: String? = null,
  @JsonProperty(ActionInputs.NAME)
  val inputs: List<Map<String, ActionInputDescriptor>> = emptyList(),
  @JsonProperty(ActionRequirements.NAME)
  val requirements: List<Map<String, ActionRequirementDescriptor>> = emptyList(),
  @JsonProperty(ActionSteps.NAME)
  val steps: List<ActionStepDescriptor> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ActionInputDescriptor(
  @JsonProperty(ActionInputType.NAME)
  val type: String? = null,
  @JsonProperty(ActionInputRequired.NAME)
  val isRequired: String? = null,
  @JsonProperty(ActionInputLabel.NAME)
  val label: String? = null,
  @JsonProperty(ActionInputDescription.NAME)
  val description: String? = null,
  @JsonProperty(ActionInputDefault.NAME)
  val defaultValue: String? = null,
  @JsonProperty(ActionInputOptions.NAME)
  val selectOptions: List<String> = emptyList(),
)

@Suppress("EnumEntryName")
enum class ActionInputTypeDescriptor {
  text,
  boolean,
  select,
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ActionRequirementDescriptor(
  @JsonProperty(ActionRequirementType.NAME)
  val type: String? = null,
  @JsonProperty(ActionRequirementValue.NAME)
  val value: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ActionStepDescriptor(
  @JsonProperty(ActionStepName.NAME)
  val name: String? = null,
  @JsonProperty(ActionStepWith.NAME)
  val with: String? = null,
  @JsonProperty(ActionStepScript.NAME)
  val script: String? = null,
  @JsonProperty(ActionStepParams.NAME)
  val parameters: Map<String, String> = emptyMap(),
)