package com.jetbrains.plugin.structure.teamcity.action.model

interface ActionStep {
  val name: String
  val parameters: Map<String, String>
}

data class RunnerBasedStep(
  override val name: String,
  override val parameters: Map<String, String>,
  val runnerName: String,
) : ActionStep

data class ActionBasedStep(
  override val name: String,
  override val parameters: Map<String, String>,
  val actionId: String,
) : ActionStep