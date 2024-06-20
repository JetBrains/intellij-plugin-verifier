package com.jetbrains.plugin.structure.teamcityAction

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionPluginManager
import com.jetbrains.plugin.structure.teamcity.action.model.ActionBasedStep
import com.jetbrains.plugin.structure.teamcity.action.model.RunnerBasedStep
import com.jetbrains.plugin.structure.teamcity.action.model.TeamCityActionPlugin
import com.jetbrains.plugin.structure.teamcityAction.Actions.someAction
import com.jetbrains.plugin.structure.teamcityAction.Steps.someScriptStep
import com.jetbrains.plugin.structure.teamcityAction.Steps.someWithStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class ParseValidActionTests(
  fileSystemType: FileSystemType,
) : BasePluginManagerTest<TeamCityActionPlugin, TeamCityActionPluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path): TeamCityActionPluginManager =
    TeamCityActionPluginManager.createManager(extractDirectory)

  @Test
  fun `parse action with valid name`() {
    val validActionNamesProvider = arrayOf("a", "aa", "a-a_a")
    validActionNamesProvider.forEach { actionName ->
      Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
      val result = createPluginSuccessfully(prepareActionYaml(someAction.copy(name = actionName)))
      with(result) {
        assertEquals(actionName, this.plugin.pluginName)
      }
    }
  }

  @Test
  fun `parse action with runner-based step`() {
    val step = someWithStep.copy(with = "runner/runnerName")

    val result = createPluginSuccessfully(prepareActionYaml(someAction.copy(steps = listOf(step))))

    with(result.plugin) {
      assertEquals(1, this.steps.size)
      with(this.steps.first()) {
        assertTrue(this is RunnerBasedStep)
        with(this as RunnerBasedStep) {
          assertEquals("runnerName", this.runnerName)
        }
      }
    }
  }

  @Test
  fun `parse action with action-based step`() {
    val step = someWithStep.copy(with = "action/actionName@1.2.3")

    val result = createPluginSuccessfully(prepareActionYaml(someAction.copy(steps = listOf(step))))

    with(result.plugin) {
      assertEquals(1, this.steps.size)
      with(this.steps.first()) {
        assertTrue(this is ActionBasedStep)
        with(this as ActionBasedStep) {
          assertEquals("actionName@1.2.3", this.actionId)
        }
      }
    }
  }

  @Test
  fun `parse action with script step`() {
    val step = someScriptStep.copy()

    val result = createPluginSuccessfully(prepareActionYaml(someAction.copy(steps = listOf(step))))

    with(result.plugin) {
      assertEquals(1, this.steps.size)
      with(this.steps.first()) {
        assertEquals(
          mapOf(
            "script.content" to step.script,
            "use.custom.script" to "true",
          ), this.parameters
        )
        assertTrue(this is RunnerBasedStep)

        with(this as RunnerBasedStep) {
          assertEquals("simpleRunner", this.runnerName)
        }
      }
    }
  }

  @Test
  fun `parse action when non-archived YAML file is provided`() {
    val yaml = temporaryFolder.newFile("action.yaml")
    val action = someAction.copy()
    Files.writeString(
      yaml,
      ObjectMapper(YAMLFactory()).registerKotlinModule().writeValueAsString(action),
    )

    val result = createPluginSuccessfully(yaml)

    with(result.plugin) {
      assertEquals(action.name, this.pluginName)
      assertEquals(action.version, this.pluginVersion)
      assertEquals(action.steps.size, this.steps.size)
      assertEquals(action.requirements.size, this.requirements.size)
      assertEquals(action.inputs.size, this.inputs.size)
    }
  }

  private fun prepareActionYaml(actionBuilder: TeamCityActionBuilder) =
    buildZipFile(temporaryFolder.newFile("plugin.zip")) {
      val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
      file("action.yaml") {
        mapper.writeValueAsString(actionBuilder)
      }
    }
}