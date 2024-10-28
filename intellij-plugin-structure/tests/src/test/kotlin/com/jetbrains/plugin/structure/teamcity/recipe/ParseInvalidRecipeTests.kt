package com.jetbrains.plugin.structure.teamcity.recipe

import com.jetbrains.plugin.structure.base.problems.InvalidSemverFormat
import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.teamcity.recipe.Inputs.someBooleanTextInput
import com.jetbrains.plugin.structure.teamcity.recipe.Inputs.someNumberInput
import com.jetbrains.plugin.structure.teamcity.recipe.Inputs.someRecipeTextInput
import com.jetbrains.plugin.structure.teamcity.recipe.Inputs.someSelectTextInput
import com.jetbrains.plugin.structure.teamcity.recipe.Recipes.someRecipe
import com.jetbrains.plugin.structure.teamcity.recipe.Requirements.someExistsRequirement
import com.jetbrains.plugin.structure.teamcity.recipe.Steps.someScriptStep
import com.jetbrains.plugin.structure.teamcity.recipe.Steps.someWithStep
import org.junit.Assert
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
  fun `recipe with unknown property in YAML`() {
    val recipeYaml = """
        name: namespace/recipe_name
        unknown_property: this property should fail deserialization
        version: 1.2.3
        description: abc
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
            "The property <namespace> (the first part of the composite `name` field) should only contain latin letters, "
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
            "The property <name> (the second part of the composite `name` field) should only contain latin letters, "
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
          propertyDescription = "the first part of the composite `name` field",
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
          propertyDescription = "the first part of the composite `name` field",
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
          propertyDescription = "the second part of the composite `name` field",
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
          propertyDescription = "the second part of the composite `name` field",
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
        InvalidSemverFormat(
          "version",
          "invalid_version",
        )
      )
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
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(description = randomAlphanumeric(251))),
      listOf(TooLongValueProblem("description", "recipe description", 251, 250)),
    )
  }

  @Test
  fun `recipe without steps`() {
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
      listOf(InvalidPropertyValueProblem("Wrong recipe input type: wrongType. Supported values are: text, boolean, number, select, password"))
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
  fun `recipe with incorrect number input`() {
    val input = someNumberInput.copy(defaultValue = "notANumber")
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(InvalidNumberProblem("default", "recipe input default value"))
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
  fun `recipe with select input and empty select options`() {
    val input = someSelectTextInput.copy(selectOptions = emptyList())
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(EmptyCollectionProblem("options", "recipe input options"))
    )
  }

  @Test
  fun `recipe with too long requirement name`() {
    val name = randomAlphanumeric(51)
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(requirements = listOf(mapOf(name to someExistsRequirement.copy())))),
      listOf(
        TooLongValueProblem("name", "recipe requirement name", 51, 50)
      )
    )
  }

  @Test
  fun `recipe with incorrect requirement type`() {
    val requirement = someExistsRequirement.copy(type = "wrong_requirement_type")
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(requirements = listOf(mapOf("req_name" to requirement)))),
      listOf(
        InvalidPropertyValueProblem(
          "Wrong recipe requirement type 'wrong_requirement_type'. " +
              "Supported values are: exists, not-exists, equals, not-equals, more-than, not-more-than, less-than, " +
              "not-less-than, starts-with, contains, does-not-contain, ends-with, matches, does-not-match, " +
              "version-more-than, version-not-more-than, version-less-than, version-not-less-than, any"
        )
      )
    )
  }

  @Test
  fun `recipe without step name`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someWithStep.copy(stepName = null)))),
      listOf(
        MissingValueProblem("name", "recipe step name")
      )
    )
  }

  @Test
  fun `recipe with non-null but empty step name`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someWithStep.copy(stepName = "")))),
      listOf(
        EmptyValueProblem("name", "recipe step name")
      )
    )
  }

  @Test
  fun `recipe with too long step name`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(
        someRecipe.copy(
          steps = listOf(
            someWithStep.copy(
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
  fun `recipe with both 'with' and 'script' properties for recipe step`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(
        someRecipe.copy(
          steps = listOf(
            someWithStep.copy(script = "echo \"hello world\"")
          )
        )
      ),
      listOf(
        PropertiesCombinationProblem(
          "The properties " +
              "<with> (runner or recipe reference) and " +
              "<script> (executable script content) " +
              "cannot be specified together for recipe step."
        )
      )
    )
  }

  @Test
  fun `recipe without 'with' and 'script' properties for recipe step`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someWithStep.copy(with = null)))),
      listOf(
        PropertiesCombinationProblem(
          "One of the properties " +
              "<with> (runner or recipe reference) or " +
              "<script> (executable script content) " +
              "should be specified for recipe step."
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
            someScriptStep.copy(
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
  fun `recipe with incorrect 'with' property for recipe step`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someWithStep.copy(with = "wrong_value")))),
      listOf(
        InvalidPropertyValueProblem(
          "The property <with> (runner or recipe reference) should be either a runner or an recipe reference. " +
              "The value should start with 'runner/' or 'recipe/' prefix"
        )
      )
    )
  }

  @Test
  fun `recipe with incorrect recipe reference in 'with' property`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someWithStep.copy(with = "recipe/recipeName")))),
      listOf(
        InvalidPropertyValueProblem(
          "The property <with> (runner or recipe reference) has an invalid recipe reference: recipeName. " +
              "The reference must follow 'recipe/name@version' format"
        )
      )
    )
  }

  @Test
  fun `recipe with unknown runner`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someWithStep.copy(with = "runner/unknown-runner")))),
      listOf(UnsupportedRunnerProblem("unknown-runner", allowedRunnerToAllowedParams.keys))
    )
  }

  @Test
  fun `recipe with one unknown runner parameter`() {
    val runner = "maven"
    val allowedParams = allowedRunnerToAllowedParams[runner]!!
    val step = someWithStep.copy(with = "runner/$runner", params = mapOf("unknownParam" to "val", "path" to "somePath"))
    val result = assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(step))),
      listOf(UnsupportedRunnerParamsProblem(runner, listOf("unknownParam"), allowedParams))
    )
    Assert.assertEquals(
      "Parameter \"unknownParam\" is not supported by $runner runner. " +
          "Supported parameters: ${allowedParams.joinUsingDoubleQuotes()}",
      result.errorsAndWarnings.first().message,
    )
  }

  @Test
  fun `recipe with multiple unknown runner parameters`() {
    val runner = "gradle"
    val allowedParams = allowedRunnerToAllowedParams[runner]!!
    val step = someWithStep.copy(with = "runner/$runner", params = mapOf("unknown1" to "val", "unknown2" to "val"))
    val result = assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(step))),
      listOf(UnsupportedRunnerParamsProblem(runner, listOf("unknown1", "unknown2"), allowedParams))
    )
    Assert.assertEquals(
      """Parameters "unknown1", "unknown2" are not supported by $runner runner. """ +
          "Supported parameters: ${allowedParams.joinUsingDoubleQuotes()}",
      result.errorsAndWarnings.first().message,
    )
  }

  @Test
  fun `recipe with empty 'script' property for recipe step`() {
    assertProblematicPlugin(
      temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(someScriptStep.copy(script = "")))),
      listOf(
        EmptyValueProblem("script", "executable script content")
      )
    )
  }

  private fun Collection<String>.joinUsingDoubleQuotes() =
    joinToString(prefix = "\"", separator = "\", \"", postfix = "\"")
}