package com.jetbrains.plugin.structure.teamcityAction

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionPluginManager
import com.jetbrains.plugin.structure.teamcity.action.model.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Path

class ParseValidFullActionTest(
  fileSystemType: FileSystemType,
) : BasePluginManagerTest<TeamCityActionPlugin, TeamCityActionPluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path): TeamCityActionPluginManager =
    TeamCityActionPluginManager.createManager(extractDirectory)

  private val actionYaml =
    """
    ---
    spec-version: 1.0.0
    name: simple-action
    version: 1.2.3
    description: this is a simple action
    inputs:
      - some text input:
          type: text
          label: text input
          default: a default text value
          required: false
          description: a description for the text input
      - some select input:
          type: select
          label: select input
          options:
            - first select option
            - second select option
          default: first select option
          required: true
          description: description for select input
      - some boolean input:
          type: boolean
          default: false
    requirements:
      - requirement 0:
          type: exists
      - requirement 1:
          type: not-exists
      - requirement 2:
          type: equals
          value: some-value
      - requirement 3:
          type: not-equals
          value: some-value
      - requirement 4:
          type: more-than
          value: 1
      - requirement 5:
          type: not-more-than
          value: 1
      - requirement 6:
          type: less-than
          value: 1
      - requirement 7:
          type: not-less-than
          value: 1
      - requirement 8:
          type: starts-with
          value: some-value
      - requirement 9:
          type: contains
          value: some-value
      - requirement 10:
          type: does-not-match
          value: some-value
      - requirement 11:
          type: version-more-than
          value: some-value
      - requirement 12:
          type: version-not-more-than
          value: some-value
      - requirement 13:
          type: version-less-than
          value: some-value
      - requirement 14:
          type: version-not-less-than
          value: some-value
    steps:
      - name: step 1
        with: runner/maven
        params:
          pomLocation: pom.xml
          goals: build
          one more param: one more value
      - script: echo "step 2 output"
        name: step 2
      - name: step 3
        with: action/name@1.2.3
        params:
          text-input: passed text parameter value
          select-input: first select option
          boolean-input: true
    """.trimIndent()

  @Test
  fun `parse full valid TeamCity Action from YAML`() {
    // arrange
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      file("action.yaml") { actionYaml }
    }

    // act
    val result = createPluginSuccessfully(pluginFile)

    // assert
    with(result.plugin) {
      assertEquals("1.0.0", this.specVersion.toString())
      assertEquals("simple-action", this.pluginName)
      assertEquals("1.2.3", this.pluginVersion)
      assertEquals("this is a simple action", this.description)

      thenInputsAreCorrect(this.inputs)
      thenRequirementsAreCorrect(this.requirements)
      thenStepsAreCorrect(this.steps)
    }
  }

  private fun thenInputsAreCorrect(inputs: List<ActionInput>) {
    assertEquals(3, inputs.size)
    with(inputs[0]) {
      assertTrue(this is TextActionInput)
      assertEquals("some text input", this.name)
      assertEquals(false, this.isRequired)
      assertEquals("text input", this.label)
      assertEquals("a description for the text input", this.description)
      assertEquals("a default text value", this.defaultValue)
    }
    with(inputs[1]) {
      assertTrue(this is SelectActionInput)
      assertEquals("some select input", this.name)
      assertEquals(true, this.isRequired)
      assertEquals("select input", this.label)
      assertEquals("description for select input", this.description)
      assertEquals("first select option", this.defaultValue)
      with(this as SelectActionInput) {
        assertEquals(listOf("first select option", "second select option"), this.selectOptions)
      }
    }
    with(inputs[2]) {
      assertTrue(this is BooleanActionInput)
      assertEquals("some boolean input", this.name)
      assertEquals(false, this.isRequired)
      assertEquals("false", this.defaultValue)
      assertNull(this.label)
      assertNull(this.description)
    }
  }

  private fun thenRequirementsAreCorrect(requirements: List<ActionRequirement>) {
    assertEquals(15, requirements.size)
    with(requirements[0]) {
      assertEquals("requirement 0", this.name)
      assertEquals(ActionRequirementType.EXISTS, this.type)
      assertNull(this.value)
    }
    with(requirements[1]) {
      assertEquals("requirement 1", this.name)
      assertEquals(ActionRequirementType.NOT_EXISTS, this.type)
      assertNull(this.value)
    }
    with(requirements[2]) {
      assertEquals("requirement 2", this.name)
      assertEquals(ActionRequirementType.EQUALS, this.type)
      assertEquals("some-value", this.value)
    }
    with(requirements[3]) {
      assertEquals("requirement 3", this.name)
      assertEquals(ActionRequirementType.NOT_EQUALS, this.type)
      assertEquals("some-value", this.value)
    }
    with(requirements[4]) {
      assertEquals("requirement 4", this.name)
      assertEquals(ActionRequirementType.MORE_THAN, this.type)
      assertEquals("1", this.value)
    }
    with(requirements[5]) {
      assertEquals("requirement 5", this.name)
      assertEquals(ActionRequirementType.NOT_MORE_THAN, this.type)
      assertEquals("1", this.value)
    }
    with(requirements[6]) {
      assertEquals("requirement 6", this.name)
      assertEquals(ActionRequirementType.LESS_THAN, this.type)
      assertEquals("1", this.value)
    }
    with(requirements[7]) {
      assertEquals("requirement 7", this.name)
      assertEquals(ActionRequirementType.NOT_LESS_THAN, this.type)
      assertEquals("1", this.value)
    }
    with(requirements[8]) {
      assertEquals("requirement 8", this.name)
      assertEquals(ActionRequirementType.STARTS_WITH, this.type)
      assertEquals("some-value", this.value)
    }
    with(requirements[9]) {
      assertEquals("requirement 9", this.name)
      assertEquals(ActionRequirementType.CONTAINS, this.type)
      assertEquals("some-value", this.value)
    }
    with(requirements[10]) {
      assertEquals("requirement 10", this.name)
      assertEquals(ActionRequirementType.DOES_NOT_MATCH, this.type)
      assertEquals("some-value", this.value)
    }
    with(requirements[11]) {
      assertEquals("requirement 11", this.name)
      assertEquals(ActionRequirementType.VERSION_MORE_THAN, this.type)
      assertEquals("some-value", this.value)
    }
    with(requirements[12]) {
      assertEquals("requirement 12", this.name)
      assertEquals(ActionRequirementType.VERSION_NOT_MORE_THAN, this.type)
      assertEquals("some-value", this.value)
    }
    with(requirements[13]) {
      assertEquals("requirement 13", this.name)
      assertEquals(ActionRequirementType.VERSION_LESS_THAN, this.type)
      assertEquals("some-value", this.value)
    }
    with(requirements[14]) {
      assertEquals("requirement 14", this.name)
      assertEquals(ActionRequirementType.VERSION_NOT_LESS_THAN, this.type)
      assertEquals("some-value", this.value)
    }
  }

  private fun thenStepsAreCorrect(steps: List<ActionStep>) {
    assertEquals(3, steps.size)
    with(steps[0]) {
      assertTrue(this is RunnerBasedStep)
      assertEquals("step 1", this.name)
      assertEquals(
        mapOf(
          "pomLocation" to "pom.xml",
          "goals" to "build",
          "one more param" to "one more value",
        ),
        this.parameters
      )
      with(this as RunnerBasedStep) {
        assertEquals("maven", this.runnerName)
      }
    }
    with(steps[1]) {
      assertTrue(this is RunnerBasedStep)
      assertEquals("step 2", this.name)
      assertEquals(
        mapOf(
          "script.content" to """echo "step 2 output"""",
          "use.custom.script" to "true",
        ),
        this.parameters
      )
      with(this as RunnerBasedStep) {
        assertEquals("simpleRunner", this.runnerName)
      }
    }
    with(steps[2]) {
      assertTrue(this is ActionBasedStep)
      assertEquals("step 3", this.name)
      assertEquals(
        mapOf(
          "text-input" to "passed text parameter value",
          "select-input" to "first select option",
          "boolean-input" to "true",
        ),
        this.parameters
      )
      with(this as ActionBasedStep) {
        assertEquals("name@1.2.3", this.actionId)
      }
    }
  }
}