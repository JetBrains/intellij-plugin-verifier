package com.jetbrains.plugin.structure.teamcity.action

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jetbrains.plugin.structure.base.problems.InvalidSemverFormat
import com.jetbrains.plugin.structure.base.problems.SemverComponentLimitExceeded
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.teamcity.action.Actions.someAction
import com.jetbrains.plugin.structure.teamcity.action.Inputs.someActionTextInput
import com.jetbrains.plugin.structure.teamcity.action.Inputs.someBooleanTextInput
import com.jetbrains.plugin.structure.teamcity.action.Inputs.someSelectTextInput
import com.jetbrains.plugin.structure.teamcity.action.Requirements.someExistsRequirement
import com.jetbrains.plugin.structure.teamcity.action.Steps.someScriptStep
import com.jetbrains.plugin.structure.teamcity.action.Steps.someWithStep
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class ParseInvalidActionTests(
  fileSystemType: FileSystemType,
) : BasePluginManagerTest<TeamCityActionPlugin, TeamCityActionPluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path): TeamCityActionPluginManager =
    TeamCityActionPluginManager.createManager(extractDirectory)

  @Test
  fun `action with multiple problems`() {
    assertProblematicPlugin(
      prepareActionYaml(
        someAction.copy(
          name = null,
          version = null,
          description = "",
          steps = emptyList(),
        )
      ),
      listOf(
        MissingValueProblem("name", "the composite action name in the 'namespace/name' format"),
        MissingValueProblem("version", "action version"),
        EmptyValueProblem("description", "action description"),
        EmptyCollectionProblem("steps", "action steps"),
      ),
    )
  }

  @Test
  fun `action without a composite name`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(name = null)),
      listOf(
        MissingValueProblem("name", "the composite action name in the 'namespace/name' format")
      ),
    )
  }

  @Test
  fun `action with the composite name in an invalid format`() {
    val invalidActionNamesProvider = arrayOf(
      "aaaaabbbbb", "/aaaaabbbbb", "aaaaabbbbb/", "/", "aaaaa/bbbbb/ccccc", "aaaaa//bbbbb", "aaaaa\\bbbbb"
    )
    invalidActionNamesProvider.forEach { actionName ->
      Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
      assertProblematicPlugin(
        prepareActionYaml(someAction.copy(name = actionName)),
        listOf(
          InvalidPropertyValueProblem(
            "The property <name> (the composite action name in the 'namespace/name' format) " +
                    "should consist of namespace and name parts. Both parts should only contain latin letters, numbers, dashes and underscores."
          )
        ),
      )
    }
  }

  @Test
  fun `action with an invalid namespace`() {
    val invalidActionNamesProvider = arrayOf(
      "-aaaaa/aaaaaa", "_aaaaa/aaaaaa", "aaaaaa-/aaaaa", "aaaaa_/aaaaa", "a--aa/aaaaa", "a__aa/aaaaa", "a+aaa/aaaaa", "абв23/aaaaa",
    )
    invalidActionNamesProvider.forEach { actionName ->
      Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
      assertProblematicPlugin(
        prepareActionYaml(someAction.copy(name = actionName)),
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
  fun `action with an invalid name`() {
    val invalidActionNamesProvider = arrayOf(
      "aaaaaa/-aaaaa", "aaaaaa/_aaaaa", "aaaaaa/aaaaaa-", "aaaaaa/aaaaa_", "aaaaaa/aa--a", "aaaaaa/aa__a", "aaaaaa/aa+aa", "aaaaaa/абв23"
    )
    invalidActionNamesProvider.forEach { actionName ->
      Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
      assertProblematicPlugin(
        prepareActionYaml(someAction.copy(name = actionName)),
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
  fun `action with a namespace that is too short`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(name = "aaaa/${randomAlphanumeric(10)}")),
      listOf(TooShortValueProblem(
        propertyName = "namespace",
        propertyDescription = "the first part of the composite `name` field",
        currentLength = 4,
        minAllowedLength = 5
      )),
    )
  }

  @Test
  fun `action with a namespace that is too long`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(name = "${randomAlphanumeric(31)}/${randomAlphanumeric(10)}")),
      listOf(TooLongValueProblem(
        propertyName = "namespace",
        propertyDescription = "the first part of the composite `name` field",
        currentLength = 31,
        maxAllowedLength = 30
      )),
    )
  }

  @Test
  fun `action with a name that is too short`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(name = "${randomAlphanumeric(10)}/aaaa")),
      listOf(TooShortValueProblem(
        propertyName = "name",
        propertyDescription = "the second part of the composite `name` field",
        currentLength = 4,
        minAllowedLength = 5
      )),
    )
  }

  @Test
  fun `action with a name that is too long`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(name = "${randomAlphanumeric(10)}/${randomAlphanumeric(31)}")),
      listOf(TooLongValueProblem(
        propertyName = "name",
        propertyDescription = "the second part of the composite `name` field",
        currentLength = 31,
        maxAllowedLength = 30
      )),
    )
  }

  @Test
  fun `action without spec_version`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(specVersion = null)),
      listOf(MissingValueProblem("spec-version", "the version of action specification")),
    )
  }

  @Test
  fun `invalid semver spec versions`() {
    val invalidSpecVersions = listOf(
      "invalid_version", "-test", "+12354", "^1.2.3", "1.1.x", "1.2.*", "~1.2.3"
    )
    invalidSpecVersions.forEach { specVersion ->
      assertProblematicPlugin(
        prepareActionYaml(someAction.copy(specVersion = specVersion)),
        listOf(
          InvalidSemverFormat(
            "spec-version",
            specVersion
          )
        )
      )
    }
  }

  @Test
  fun `invalid spec version limits`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(specVersion = "10000")),
      listOf(SemverComponentLimitExceeded(
        componentName = "major",
        versionName = "spec-version",
        version = "10000",
        limit = TeamCityActionSpecVersionUtils.MAX_MAJOR_VALUE - 1
      ))
    )

    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(specVersion = "0.10000")),
      listOf(SemverComponentLimitExceeded(
        componentName = "minor",
        versionName = "spec-version",
        version = "0.10000",
        limit = TeamCityActionSpecVersionUtils.VERSION_MINOR_LENGTH - 1
      ))
    )

    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(specVersion = "0.0.10000")),
      listOf(SemverComponentLimitExceeded(
        componentName = "patch",
        versionName = "spec-version",
        version = "0.0.10000",
        limit = TeamCityActionSpecVersionUtils.VERSION_PATCH_LENGTH - 1
      ))
    )
  }

  @Test
  fun `action without version`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(version = null)),
      listOf(MissingValueProblem("version", "action version")),
    )
  }

  @Test
  fun `action with invalid version`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(version = "invalid_version")),
      listOf(
        InvalidSemverFormat(
          "version",
          "invalid_version",
        )
      )
    )
  }

  @Test
  fun `action without description`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(description = null)),
      listOf(MissingValueProblem("description", "action description")),
    )
  }

  @Test
  fun `action with non-null but empty description`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(description = "")),
      listOf(EmptyValueProblem("description", "action description")),
    )
  }

  @Test
  fun `action with too long description`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(description = randomAlphanumeric(251))),
      listOf(TooLongValueProblem("description", "action description", 251, 250)),
    )
  }

  @Test
  fun `action without steps`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(steps = listOf())),
      listOf(EmptyCollectionProblem("steps", "action steps"))
    )
  }

  @Test
  fun `action with too long input name`() {
    val name = randomAlphanumeric(51)
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(inputs = listOf(mapOf(name to someActionTextInput.copy())))),
      listOf(TooLongValueProblem("name", "action input name", 51, 50))
    )
  }

  @Test
  fun `action with incorrect input type`() {
    val input = someActionTextInput.copy(type = "wrongType")
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(InvalidPropertyValueProblem("Wrong action input type: wrongType. Supported values are: text, boolean, select"))
    )
  }

  @Test
  fun `action with incorrect input 'required' boolean flag`() {
    val input = someActionTextInput.copy(required = "not boolean")
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(InvalidBooleanProblem("required", "indicates whether the input is required"))
    )
  }

  @Test
  fun `action with non-null but empty input label`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(inputs = listOf(mapOf("input_name" to someActionTextInput.copy(label = ""))))),
      listOf(EmptyValueProblem("label", "action input label"))
    )
  }

  @Test
  fun `action with too long input label`() {
    val input = someActionTextInput.copy(label = randomAlphanumeric(101))
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(TooLongValueProblem("label", "action input label", 101, 100))
    )
  }

  @Test
  fun `action with non-null but empty input description`() {
    val input = someActionTextInput.copy(description = "")
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(EmptyValueProblem("description", "action input description"))
    )
  }

  @Test
  fun `action with too long input description`() {
    val input = someActionTextInput.copy(description = randomAlphanumeric(251))
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(TooLongValueProblem("description", "action input description", 251, 250))
    )
  }

  @Test
  fun `action with boolean input and incorrect boolean default value`() {
    val input = someBooleanTextInput.copy(defaultValue = "not boolean")
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(InvalidBooleanProblem("default", "action input default value"))
    )
  }

  @Test
  fun `action with select input and empty select options`() {
    val input = someSelectTextInput.copy(selectOptions = emptyList())
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(inputs = listOf(mapOf("input_name" to input)))),
      listOf(EmptyCollectionProblem("options", "action input options"))
    )
  }

  @Test
  fun `action with too long requirement name`() {
    val name = randomAlphanumeric(51)
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(requirements = listOf(mapOf(name to someExistsRequirement.copy())))),
      listOf(
        TooLongValueProblem("name", "action requirement name", 51, 50)
      )
    )
  }

  @Test
  fun `action with incorrect requirement type`() {
    val requirement = someExistsRequirement.copy(type = "wrong_requirement_type")
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(requirements = listOf(mapOf("req_name" to requirement)))),
      listOf(
        InvalidPropertyValueProblem(
          "Wrong action requirement type 'wrong_requirement_type'. " +
              "Supported values are: exists, not-exists, equals, not-equals, more-than, not-more-than, less-than, " +
              "not-less-than, starts-with, contains, does-not-contain, ends-with, matches, does-not-match, " +
              "version-more-than, version-not-more-than, version-less-than, version-not-less-than, any"
        )
      )
    )
  }

  @Test
  fun `action without step name`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(steps = listOf(someWithStep.copy(stepName = null)))),
      listOf(
        MissingValueProblem("name", "action step name")
      )
    )
  }

  @Test
  fun `action with non-null but empty step name`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(steps = listOf(someWithStep.copy(stepName = "")))),
      listOf(
        EmptyValueProblem("name", "action step name")
      )
    )
  }

  @Test
  fun `action with too long step name`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(steps = listOf(someWithStep.copy(stepName = randomAlphanumeric(51))))),
      listOf(
        TooLongValueProblem("name", "action step name", 51, 50)
      )
    )
  }

  @Test
  fun `action with both 'with' and 'script' properties for action step`() {
    assertProblematicPlugin(
      prepareActionYaml(
        someAction.copy(
          steps = listOf(
            someWithStep.copy(script = "echo \"hello world\"")
          )
        )
      ),
      listOf(
        PropertiesCombinationProblem(
          "The properties " +
              "<with> (runner or action reference) and " +
              "<script> (executable script content) " +
              "cannot be specified together for action step."
        )
      )
    )
  }

  @Test
  fun `action without 'with' and 'script' properties for action step`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(steps = listOf(someWithStep.copy(with = null)))),
      listOf(
        PropertiesCombinationProblem(
          "One of the properties " +
              "<with> (runner or action reference) or " +
              "<script> (executable script content) " +
              "should be specified for action step."
        )
      )
    )
  }

  @Test
  fun `action with too long 'with' property for action step`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(steps = listOf(someWithStep.copy(with = "runner/" + randomAlphanumeric(94))))),
      listOf(
        TooLongValueProblem("with", "runner or action reference", 101, 100)
      )
    )
  }

  @Test
  fun `action with too long 'script' property for action step`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(steps = listOf(someScriptStep.copy(script = randomAlphanumeric(50_001))))),
      listOf(
        TooLongValueProblem("script", "executable script content", 50_001, 50_000)
      )
    )
  }

  @Test
  fun `action with incorrect 'with' property for action step`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(steps = listOf(someWithStep.copy(with = "wrong_value")))),
      listOf(
        InvalidPropertyValueProblem(
          "The property <with> (runner or action reference) should have a value " +
              "starting with one of the following prefixes: runner/, action/"
        )
      )
    )
  }

  @Test
  fun `action with empty 'script' property for action step`() {
    assertProblematicPlugin(
      prepareActionYaml(someAction.copy(steps = listOf(someScriptStep.copy(script = "")))),
      listOf(
        EmptyValueProblem("script", "executable script content")
      )
    )
  }

  private fun prepareActionYaml(actionBuilder: TeamCityActionBuilder) =
    buildZipFile(temporaryFolder.newFile("plugin-${UUID.randomUUID()}.zip")) {
      val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
      file("action.yaml") {
        mapper.writeValueAsString(actionBuilder)
      }
    }
}