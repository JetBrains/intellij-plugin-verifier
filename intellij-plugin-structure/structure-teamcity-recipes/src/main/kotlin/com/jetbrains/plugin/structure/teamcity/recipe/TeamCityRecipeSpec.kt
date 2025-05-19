package com.jetbrains.plugin.structure.teamcity.recipe

object TeamCityRecipeSpec {
  object RecipeCompositeName {
    const val NAME = "name"
    const val DESCRIPTION = "the composite recipe name in the 'namespace/name' format"

    // Regular expression for the recipe's composite name – the namespace and the name separated by '/'
    val compositeNameRegex: Regex = Regex("^([^/]+)/([^/]+)$")

    /**
     * Regular expression pattern for both the recipe's namespace and the recipe's name.
     *
     * The pattern enforces the following rules for both namespace and id:
     * - cannot be empty.
     * – can only contain latin letters, dashes and underscores.
     * - cannot start or end with a dash or underscore.
     * - cannot contain several consecutive dashes or underscores.
     */
    val meaningfulPartRegex: Regex = Regex("^[a-zA-Z0-9]+([_-][a-zA-Z0-9]+)*\$")

    object Namespace {
      const val NAME = "namespace"
      const val DESCRIPTION = "the first part of the composite `${RecipeCompositeName.NAME}` property"
      const val MIN_LENGTH = 5
      const val MAX_LENGTH = 30
    }

    object NameInNamespace {
      const val NAME = "name"
      const val DESCRIPTION = "the second part of the composite `${RecipeCompositeName.NAME}` property"
      const val MIN_LENGTH = 5
      const val MAX_LENGTH = 30
    }

    fun getNamespace(recipeName: String?): String? = getNamespaceAndNameGroups(recipeName)?.get(1)

    fun getNameInNamespace(recipeName: String?): String? = getNamespaceAndNameGroups(recipeName)?.get(2)

    private fun getNamespaceAndNameGroups(recipeName: String?): List<String>? {
      if (recipeName == null) return null
      val matchResult = compositeNameRegex.matchEntire(recipeName)
      return matchResult?.groupValues
    }
  }

  object RecipeVersion {
    const val NAME = "version"
    const val DESCRIPTION = "recipe version"
  }

  object RecipeTitle {
    const val NAME = "title"
    const val DESCRIPTION = "recipe title"
    const val MAX_LENGTH = 50
  }

  object RecipeDescription {
    const val NAME = "description"
    const val DESCRIPTION = "recipe description"
    const val MAX_LENGTH = 250
  }

  object RecipeInputs {
    const val NAME = "inputs"
  }

  object RecipeInputName {
    const val NAME = "name"
    const val DESCRIPTION = "recipe input name"
    const val MAX_LENGTH = 50
  }

  object RecipeInputType {
    const val NAME = "type"
    const val DESCRIPTION = "recipe input type"
  }

  object RecipeInputRequired {
    const val NAME = "required"
    const val DESCRIPTION = "indicates whether the input is required"
  }

  object RecipeInputLabel {
    const val NAME = "label"
    const val DESCRIPTION = "recipe input label"
    const val MAX_LENGTH = 100
  }

  object RecipeInputDescription {
    const val NAME = "description"
    const val DESCRIPTION = "recipe input description"
    const val MAX_LENGTH = 250
  }

  object RecipeInputDefault {
    const val NAME = "default"
    const val DESCRIPTION = "recipe input default value"
  }

  object RecipeInputOptions {
    const val NAME = "options"
    const val DESCRIPTION = "recipe input options"
  }

  object RecipeSteps {
    const val NAME = "steps"
    const val DESCRIPTION = "recipe steps"
  }

  object RecipeStepName {
    const val NAME = "name"
    const val DESCRIPTION = "recipe step name"
    const val MAX_LENGTH = 50
  }

  object RecipeStepReference {
    const val NAME = "uses"
    const val DESCRIPTION = "recipe reference"
    const val MAX_LENGTH = 100
  }

  object RecipeStepCommandLineScript {
    const val NAME = "script"
    const val DESCRIPTION = "executable script content"
    const val MAX_LENGTH = 50_000
  }

  object RecipeStepKotlinScript {
    const val NAME = "kotlin-script"
    const val DESCRIPTION = "Kotlin script content"
    const val MAX_LENGTH = 50_000
  }

  object RecipeStepInputs {
    const val NAME = "inputs"
  }

  object RecipeContainer {
    const val NAME = "container"
  }

  object RecipeContainerImage {
    const val NAME = "image"
    const val DESCRIPTION = "container image name"
  }

  object RecipeContainerImagePlatform {
    const val NAME = "platform"
  }

  object RecipeContainerRunParameters {
    const val NAME = "parameters"
    const val DESCRIPTION = "container run options"
  }
}