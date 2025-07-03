package com.jetbrains.plugin.structure.teamcity.recipe

import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.teamcity.recipe.Inputs.someBooleanTextInput
import com.jetbrains.plugin.structure.teamcity.recipe.Inputs.someRecipeTextInput
import com.jetbrains.plugin.structure.teamcity.recipe.Inputs.someSelectTextInput
import com.jetbrains.plugin.structure.teamcity.recipe.Recipes.someRecipe
import com.jetbrains.plugin.structure.teamcity.recipe.Steps.someCommandLineScriptStep
import com.jetbrains.plugin.structure.teamcity.recipe.Steps.someKotlinScriptStep
import com.jetbrains.plugin.structure.teamcity.recipe.Steps.someUsesStep
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class ParseInvalidRecipeTests(
  fileSystemType: FileSystemType,
) : BasePluginManagerTest<TeamCityRecipePlugin, TeamCityRecipePluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path): TeamCityRecipePluginManager =
    TeamCityRecipePluginManager.createManager(extractDirectory)

  @Test
  fun `recipe with incorrect YAML`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml("some random text"),
      listOf(ParseYamlProblem),
    )
  }

  @Test
  fun `recipe YAML with duplicate properties`() {
    val recipeYaml = """
            name: aaa
            name: bbb
        """.trimIndent()

    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(recipeYaml),
      listOf(DuplicatePropertiesProblem),
    )
  }

  @Test
  fun `recipe with unknown property in YAML`() {
    val recipeYaml = """
        name: namespace/recipe_name
        unknown_property: this property should fail deserialization
        version: 1.2.3
        description: abc
        inputs:
          - some input:
              type: text
              required: true
        steps:
          - name: step_1
            script: echo "kek"
        """.trimIndent()
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(recipeYaml),
      listOf(UnknownPropertyProblem("unknown_property")),
    )
  }

  @Test
  fun `input with unknown property in YAML`() {
    val recipeYaml = """
        name: namespace/recipe_name
        version: 1.2.3
        description: abc
        inputs:
          - some input:
              type: text
              required: true
              unknown_property: this property should fail deserialization
        steps:
          - name: step_1
            script: echo "kek"
        """.trimIndent()
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(recipeYaml),
      listOf(UnknownPropertyProblem("unknown_property")),
    )
  }

  @Test
  fun `step with unknown property in YAML`() {
    val recipeYaml = """
        name: namespace/recipe_name
        version: 1.2.3
        description: abc
        steps:
          - name: step_1
            script: echo "kek"
            unknown_property: this property should fail deserialization
        """.trimIndent()
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(recipeYaml),
      listOf(UnknownPropertyProblem("unknown_property")),
    )
  }

  @Test
  fun `recipe with multiple problems`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(
        someRecipe.copy(
          name = null,
          version = null,
          description = "",
          steps = emptyList(),
        )
      ),
      listOf(
        MissingValueProblem("name", "the composite recipe name in the 'namespace/name' format"),
        MissingValueProblem("version", "recipe version"),
        EmptyValueProblem("description", "recipe description"),
        EmptyCollectionProblem("steps", "recipe steps"),
      ),
    )
  }

  @Test
  fun `recipe without a composite name`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(name = null)),
      listOf(
        MissingValueProblem("name", "the composite recipe name in the 'namespace/name' format")
      ),
    )
  }

  @Test
  fun `recipe with the composite name in an invalid format`() {
    val invalidRecipeNamesProvider = arrayOf(
      "aaaaabbbbb", "/aaaaabbbbb", "aaaaabbbbb/", "/", "aaaaa/bbbbb/ccccc", "aaaaa//bbbbb", "aaaaa\\bbbbb"
    )
    invalidRecipeNamesProvider.forEach { recipeName ->
      Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
      assertProblematicPlugin(
        temporaryFolder.prepareRecipeYaml(someRecipe.copy(name = recipeName)),
        listOf(
          InvalidPropertyValueProblem(
            "The property <name> (the composite recipe name in the 'namespace/name' format) " +
                "should consist of namespace and name parts. Both parts should only contain latin letters, numbers, dashes and underscores."
          )
        ),
      )
    }
  }

  @Test
  fun `recipe with an invalid namespace`() {
    val invalidRecipeNamesProvider = arrayOf(
      "-aaaaa/aaaaaa",
      "_aaaaa/aaaaaa",
      "aaaaaa-/aaaaa",
      "aaaaa_/aaaaa",
      "a--aa/aaaaa",
      "a__aa/aaaaa",
      "a+aaa/aaaaa",
      "абв23/aaaaa",
    )
    invalidRecipeNamesProvider.forEach { recipeName ->
      Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
      assertProblematicPlugin(
        temporaryFolder.prepareRecipeYaml(someRecipe.copy(name = recipeName)),
        listOf(
          InvalidPropertyValueProblem(
            "The property <namespace> (the first part of the composite `name` property) should only contain latin letters, "
                + "numbers, dashes and underscores. The property cannot start or end with a dash or underscore, and "
                + "cannot contain several consecutive dashes and underscores."
          )
        ),
      )
    }
  }

  @Test
  fun `recipe with an invalid name`() {
    val invalidRecipeNamesProvider = arrayOf(
      "aaaaaa/-aaaaa",
      "aaaaaa/_aaaaa",
      "aaaaaa/aaaaaa-",
      "aaaaaa/aaaaa_",
      "aaaaaa/aa--a",
      "aaaaaa/aa__a",
      "aaaaaa/aa+aa",
      "aaaaaa/абв23"
    )
    invalidRecipeNamesProvider.forEach { recipeName ->
      Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
      assertProblematicPlugin(
        temporaryFolder.prepareRecipeYaml(someRecipe.copy(name = recipeName)),
        listOf(
          InvalidPropertyValueProblem(
            "The property <name> (the second part of the composite `name` property) should only contain latin letters, "
                + "numbers, dashes and underscores. The property cannot start or end with a dash or underscore, and "
                + "cannot contain several consecutive dashes and underscores."
          )
        ),
      )
    }
  }

  @Test
  fun `recipe with a namespace that is too short`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(name = "aaaa/${randomAlphanumeric(10)}")),
      listOf(
        TooShortValueProblem(
          propertyName = "namespace",
          propertyDescription = "the first part of the composite `name` property",
          currentLength = 4,
          minAllowedLength = 5
        )
      ),
    )
  }

  @Test
  fun `recipe with a namespace that is too long`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(name = "${randomAlphanumeric(31)}/${randomAlphanumeric(10)}")),
      listOf(
        TooLongValueProblem(
          propertyName = "namespace",
          propertyDescription = "the first part of the composite `name` property",
          currentLength = 31,
          maxAllowedLength = 30
        )
      ),
    )
  }

  @Test
  fun `recipe with a name that is too short`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(name = "${randomAlphanumeric(10)}/aaaa")),
      listOf(
        TooShortValueProblem(
          propertyName = "name",
          propertyDescription = "the second part of the composite `name` property",
          currentLength = 4,
          minAllowedLength = 5
        )
      ),
    )
  }

  @Test
  fun `recipe with a name that is too long`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(name = "${randomAlphanumeric(10)}/${randomAlphanumeric(31)}")),
      listOf(
        TooLongValueProblem(
          propertyName = "name",
          propertyDescription = "the second part of the composite `name` property",
          currentLength = 31,
          maxAllowedLength = 30
        )
      ),
    )
  }

  @Test
  fun `recipe without version`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(version = null)),
      listOf(MissingValueProblem("version", "recipe version")),
    )
  }

  @Test
  fun `recipe with invalid version`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(version = "invalid_version")),
      listOf(
        InvalidPropertyValueProblem("The property <version> (recipe version) has an invalid value. " +
                "The version must be in the '<major>.<minor>.<patch>' format.")
      )
    )
  }

  @Test
  fun `recipe without title`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(title = null)),
      listOf(MissingValueProblem("title", "recipe title")),
    )
  }

  @Test
  fun `recipe with non-null but empty title`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(title = "")),
      listOf(EmptyValueProblem("title", "recipe title")),
    )
  }

  @Test
  fun `recipe with too long title`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(title = randomAlphanumeric(51))),
      listOf(TooLongValueProblem("title", "recipe title", 51, 50)),
    )
  }

  @Test
  fun `recipe without description`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(description = null)),
      listOf(MissingValueProblem("description", "recipe description")),
    )
  }

  @Test
  fun `recipe with non-null but empty description`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(description = "")),
      listOf(EmptyValueProblem("description", "recipe description")),
    )
  }

  @Test
  fun `recipe with too long description`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(description = randomAlphanumeric(1001))),
      listOf(TooLongValueProblem("description", "recipe description", 1001, 1000)),
    )
  }

  @Test
  fun `recipe without steps`() {
    val missingSteps = arrayOf(
      "",
      "steps: null",
    )
    missingSteps.forEach { steps ->
      val recipeYaml = """
            ---
            name: "recipe_namespace/recipe_name"
            version: "1.2.3"
            title: "title"
            description: "description"
            $steps
        """.trimIndent()
      assertProblematicPlugin(
        temporaryFolder.prepareRecipeYaml(recipeYaml),
        listOf(MissingValueProblem("steps", "recipe steps"))
      )
    }
  }

  @Test
  fun `recipe with empty step collection`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf())),
      listOf(EmptyCollectionProblem("steps", "recipe steps"))
    )
  }

  @Test
  fun `recipe with too long input name`() {
    val name = randomAlphanumeric(51)
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(inputs = listOf(mapOf(name to someRecipeTextInput.copy())))),
      listOf(TooLongValueProblem("name", "recipe input name", 51, 50))
    )
  }

  @Test
  fun `recipe with incorrect input type`() {
    val input = someRecipeTextInput.copy(type = "wrongType")
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(InvalidPropertyValueProblem("Wrong recipe input type: wrongType. Supported values are: text, boolean, select, password"))
    )
  }

  @Test
  fun `recipe with incorrect input 'required' boolean flag`() {
    val input = someRecipeTextInput.copy(required = "not boolean")
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(InvalidBooleanProblem("required", "indicates whether the input is required"))
    )
  }

  @Test
  fun `recipe with non-null but empty input label`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(
        someRecipe.copy(
          inputs = listOf(
            mapOf(
              "input_name" to someRecipeTextInput.copy(
                label = ""
              )
            )
          )
        )
      ),
      listOf(EmptyValueProblem("label", "recipe input label"))
    )
  }

  @Test
  fun `recipe with too long input label`() {
    val input = someRecipeTextInput.copy(label = randomAlphanumeric(101))
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(TooLongValueProblem("label", "recipe input label", 101, 100))
    )
  }

  @Test
  fun `recipe with non-null but empty input description`() {
    val input = someRecipeTextInput.copy(description = "")
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(EmptyValueProblem("description", "recipe input description"))
    )
  }

  @Test
  fun `recipe with too long input description`() {
    val input = someRecipeTextInput.copy(description = randomAlphanumeric(251))
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(TooLongValueProblem("description", "recipe input description", 251, 250))
    )
  }

  @Test
  fun `recipe with boolean input and incorrect boolean default value`() {
    val input = someBooleanTextInput.copy(defaultValue = "not boolean")
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(InvalidBooleanProblem("default", "recipe input default value"))
    )
  }

  @Test
  fun `recipe with select input and missing select options`() {
    val missingOptions = arrayOf(
      "",
      "options: null",
    )
    missingOptions.forEach { options ->
      val recipeYaml = """
            ---
            name: "recipe_namespace/recipe_name"
            version: "1.2.3"
            title: "title"
            description: "description"
            inputs:
            - input_name:
                type: "select"
                $options
            steps:
            - name: "step name"
              script: "exit 0"
        """.trimIndent()
      assertProblematicPlugin(
        temporaryFolder.prepareRecipeYaml(recipeYaml),
        listOf(MissingValueProblem("options", "recipe input options"))
      )
    }
  }

  @Test
  fun `recipe with select input and empty select options`() {
    val input = someSelectTextInput.copy(selectOptions = emptyList())
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(EmptyCollectionProblem("options", "recipe input options"))
    )
  }

  @Test
  fun `recipe without step name`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someUsesStep.copy(stepName = null)))),
      listOf(
        MissingValueProblem("name", "recipe step name")
      )
    )
  }

  @Test
  fun `recipe with non-null but empty step name`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someUsesStep.copy(stepName = "")))),
      listOf(
        EmptyValueProblem("name", "recipe step name")
      )
    )
  }

  @Test
  fun `recipe with a too long step name`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(
        someRecipe.copy(
          steps = listOf(
            someUsesStep.copy(
              stepName = randomAlphanumeric(
                51
              )
            )
          )
        )
      ),
      listOf(
        TooLongValueProblem("name", "recipe step name", 51, 50)
      )
    )
  }

  @Test
  fun `recipe with both 'script' and 'uses' properties for recipe step`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(
        someRecipe.copy(
          steps = listOf(
            someUsesStep.copy(script = "echo \"hello world\"")
          )
        )
      ),
      listOf(
        PropertiesCombinationProblem(
          "The properties " +
              "<script> (executable script content), " +
              "<kotlin-script> (Kotlin script content) " +
              "and <uses> (recipe reference) " +
              "cannot be specified together in a recipe step."
        )
      )
    )
  }

  @Test
  fun `recipe with both 'script' and 'kotlin-script' properties for recipe step`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(
        someRecipe.copy(
          steps = listOf(
            someKotlinScriptStep.copy(script = "echo \"hello world\"")
          )
        )
      ),
      listOf(
        PropertiesCombinationProblem(
          "The properties " +
                  "<script> (executable script content), " +
                  "<kotlin-script> (Kotlin script content) " +
                  "and <uses> (recipe reference) " +
                  "cannot be specified together in a recipe step."
        )
      )
    )
  }

  @Test
  fun `recipe with both 'kotlin-script' and 'uses' properties for recipe step`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(
        someRecipe.copy(
          steps = listOf(
            someUsesStep.copy(kotlinScript = "print(\"hi\")")
          )
        )
      ),
      listOf(
        PropertiesCombinationProblem(
          "The properties " +
                  "<script> (executable script content), " +
                  "<kotlin-script> (Kotlin script content) " +
                  "and <uses> (recipe reference) " +
                  "cannot be specified together in a recipe step."
        )
      )
    )
  }

  @Test
  fun `recipe without 'script', 'kotlin-script' and 'uses' properties for recipe step`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someUsesStep.copy(uses = null)))),
      listOf(
        PropertiesCombinationProblem(
          "Either <script> (executable script content), " +
          "<kotlin-script> (Kotlin script content) or " +
          "<uses> (recipe reference) " +
          "should be specified in a recipe step."
        )
      )
    )
  }

  @Test
  fun `recipe with too long 'script' property for recipe step`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(
        someRecipe.copy(
          steps = listOf(
            someCommandLineScriptStep.copy(
              script = randomAlphanumeric(
                50_001
              )
            )
          )
        )
      ),
      listOf(
        TooLongValueProblem("script", "executable script content", 50_001, 50_000)
      )
    )
  }

  @Test
  fun `recipe with too long 'kotlin-script' property for recipe step`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(
        someRecipe.copy(
          steps = listOf(
            someKotlinScriptStep.copy(
              kotlinScript = randomAlphanumeric(
                50_001
              )
            )
          )
        )
      ),
      listOf(
        TooLongValueProblem("kotlin-script", "Kotlin script content", 50_001, 50_000)
      )
    )
  }

  @Test
  fun `recipe with incorrect 'with' property for recipe step`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someUsesStep.copy(uses = "wrong_value")))),
      listOf(
        InvalidPropertyValueProblem(
          "The property <uses> (recipe reference) has an invalid recipe reference: wrong_value. " +
              "The reference must follow the '<namespace>/<name>@<version>' format."
        )
      )
    )
  }

  @Test
  fun `recipe with incorrect recipe reference in 'uses' property`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someUsesStep.copy(uses = "recipe/recipeName")))),
      listOf(
        InvalidPropertyValueProblem(
          "The property <uses> (recipe reference) has an invalid recipe reference: recipe/recipeName. " +
              "The reference must follow the '<namespace>/<name>@<version>' format."
        )
      )
    )
  }

  @Test
  fun `recipe with empty 'script' property for recipe step`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someCommandLineScriptStep.copy(script = "")))),
      listOf(
        EmptyValueProblem("script", "executable script content")
      )
    )
  }

  @Test
  fun `recipe with empty 'kotlin-script' property for recipe step`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someKotlinScriptStep.copy(kotlinScript = "")))),
      listOf(
        EmptyValueProblem("kotlin-script", "Kotlin script content")
      )
    )
  }
}