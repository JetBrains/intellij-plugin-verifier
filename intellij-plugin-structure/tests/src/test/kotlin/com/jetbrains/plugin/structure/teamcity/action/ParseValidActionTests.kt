package com.jetbrains.plugin.structure.teamcity.action

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.teamcity.action.Actions.someAction
import com.jetbrains.plugin.structure.teamcity.action.Steps.someScriptStep
import com.jetbrains.plugin.structure.teamcity.action.Steps.someWithStep
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class ParseValidActionTests(
  fileSystemType: FileSystemType,
) : BasePluginManagerTest<TeamCityActionPlugin, TeamCityActionPluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path): TeamCityActionPluginManager =
    TeamCityActionPluginManager.createManager(extractDirectory)

  @Test
  fun `parse action with valid name`() {
    val validActionNamesProvider = arrayOf("aaaaa/aaaaa", "aaaaa/a-a_a", "a-a_a/aaaaa", "${randomAlphanumeric(30)}/${randomAlphanumeric(30)}")
    validActionNamesProvider.forEach { actionName ->
      Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
      val result = createPluginSuccessfully(prepareActionYaml(someAction.copy(name = actionName)))
      with(result) {
        assertEquals(actionName, this.plugin.pluginName)
      }
    }
  }

  @Test
  fun `parse action id and namespace`() {
    val expectedNamespace = "jetbrains"
    val expectedActionId = "test_action"
    val expectedActionName = "$expectedNamespace/$expectedActionId"

    Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
    val result = createPluginSuccessfully(prepareActionYaml(someAction.copy(name = expectedActionName)))
    with(result) {
      assertEquals(expectedActionName, this.plugin.pluginName)
      assertEquals(expectedActionName, this.plugin.pluginId)
      assertEquals(expectedNamespace, this.plugin.namespace)
    }
  }

  @Test
  fun `parse action with runner-based step`() {
    val runners = listOf("gradle", "maven", "node-js", "command-line", "unity")
    runners.forEach { runnerName ->
      val step = someWithStep.copy(with = "runner/$runnerName")
      createPluginSuccessfully(prepareActionYaml(someAction.copy(steps = listOf(step))))
    }
  }

  @Test
  fun `parse action with action-based step`() {
    val step = someWithStep.copy(with = "action/actionName@1.2.3")
    createPluginSuccessfully(prepareActionYaml(someAction.copy(steps = listOf(step))))
  }

  @Test
  fun `parse action with script step`() {
    val step = someScriptStep.copy()
    createPluginSuccessfully(prepareActionYaml(someAction.copy(steps = listOf(step))))
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
    }
  }

  private fun prepareActionYaml(actionBuilder: TeamCityActionBuilder) =
    buildZipFile(temporaryFolder.newFile("plugin-${UUID.randomUUID()}.zip")) {
      val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
      file("action.yaml") {
        mapper.writeValueAsString(actionBuilder)
      }
    }
}