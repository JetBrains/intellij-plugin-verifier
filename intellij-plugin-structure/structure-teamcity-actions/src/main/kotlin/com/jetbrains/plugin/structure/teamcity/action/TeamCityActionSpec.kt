package com.jetbrains.plugin.structure.teamcity.action

object TeamCityActionSpec {
  object ActionSpecVersion {
    const val NAME = "spec-version"
    const val DESCRIPTION = "the version of action specification"
  }

  object ActionName {
    const val NAME = "name"
    const val DESCRIPTION = "the composite action name that consists of two parts: the `namespace` and the `ID` divided by the `/` symbol"

    const val NAMESPACE_SPLIT_SYMBOL = "/"

    /**
     * Regular expression pattern for both action namespace and id.
     *
     * The pattern enforces the following rules for both namespace and id:
     * - cannot be empty.
     * – can only contain latin letters, dashes and underscores.
     * - cannot start or end with a dash or underscore.
     * - cannot contain several consecutive dashes or underscores.
     */
    private const val ID_AND_NAMESPACE_PATTERN = "^[a-zA-Z0-9]+([_-][a-zA-Z0-9]+)*\$"
    val idAndNamespaceRegex: Regex = Regex(ID_AND_NAMESPACE_PATTERN)

    object Namespace {
      const val NAME = "namespace"
      const val DESCRIPTION = "the first part of the composite `${ActionName.NAME}` field"
      const val MIN_LENGTH = 5
      const val MAX_LENGTH = 30
    }

    object ID {
      const val NAME = "id"
      const val DESCRIPTION = "the second part of the composite `${ActionName.NAME}` field"
      const val MIN_LENGTH = 5
      const val MAX_LENGTH = 30
    }
  }

  object ActionVersion {
    const val NAME = "version"
    const val DESCRIPTION = "action version"
  }

  object ActionDescription {
    const val NAME = "description"
    const val DESCRIPTION = "action description"
    const val MAX_LENGTH = 250
  }

  object ActionInputs {
    const val NAME = "inputs"
  }

  object ActionInputName {
    const val NAME = "name"
    const val DESCRIPTION = "action input name"
    const val MAX_LENGTH = 50
  }

  object ActionInputType {
    const val NAME = "type"
    const val DESCRIPTION = "action input type"
  }

  object ActionInputRequired {
    const val NAME = "required"
    const val DESCRIPTION = "indicates whether the input is required"
  }

  object ActionInputLabel {
    const val NAME = "label"
    const val DESCRIPTION = "action input label"
    const val MAX_LENGTH = 100
  }

  object ActionInputDescription {
    const val NAME = "description"
    const val DESCRIPTION = "action input description"
    const val MAX_LENGTH = 250
  }

  object ActionInputDefault {
    const val NAME = "default"
    const val DESCRIPTION = "action input default value"
  }

  object ActionInputOptions {
    const val NAME = "options"
    const val DESCRIPTION = "action input options"
  }

  object ActionRequirements {
    const val NAME = "requirements"
  }

  object ActionRequirementName {
    const val NAME = "name"
    const val DESCRIPTION = "action requirement name"
    const val MAX_LENGTH = 50
  }

  object ActionRequirementType {
    const val NAME = "type"
    const val DESCRIPTION = "action requirement type"
  }

  object ActionRequirementValue {
    const val NAME = "value"
  }

  object ActionSteps {
    const val NAME = "steps"
    const val DESCRIPTION = "action steps"
  }

  object ActionStepName {
    const val NAME = "name"
    const val DESCRIPTION = "action step name"
    const val MAX_LENGTH = 50
  }

  object ActionStepWith {
    const val NAME = "with"
    const val DESCRIPTION = "runner or action reference"
    const val MAX_LENGTH = 100
    val allowedPrefixes = listOf("runner/", "action/")
  }

  object ActionStepScript {
    const val NAME = "script"
    const val DESCRIPTION = "executable script content"
    const val MAX_LENGTH = 50_000
  }

  object ActionStepParams {
    const val NAME = "params"
  }
}