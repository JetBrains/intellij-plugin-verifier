package com.jetbrains.plugin.structure.teamcity.action.model

interface ActionInput {
  val name: String
  val isRequired: Boolean
  val label: String?
  val description: String?
  val defaultValue: String?
}

data class TextActionInput(
  override val name: String,
  override val isRequired: Boolean,
  override val label: String? = null,
  override val description: String? = null,
  override val defaultValue: String? = null,
) : ActionInput

data class BooleanActionInput(
  override val name: String,
  override val isRequired: Boolean,
  override val label: String? = null,
  override val description: String? = null,
  override val defaultValue: String? = null,
) : ActionInput

data class SelectActionInput(
  override val name: String,
  override val isRequired: Boolean,
  override val label: String? = null,
  override val description: String? = null,
  override val defaultValue: String? = null,
  val selectOptions: List<String>,
) : ActionInput