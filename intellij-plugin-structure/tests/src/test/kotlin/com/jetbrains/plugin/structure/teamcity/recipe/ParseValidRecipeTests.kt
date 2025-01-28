package com.jetbrains.plugin.structure.teamcity.recipe

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.teamcity.recipe.Recipes.someRecipe
import com.jetbrains.plugin.structure.teamcity.recipe.Steps.someCommandLineScriptStep
import com.jetbrains.plugin.structure.teamcity.recipe.Steps.someKotlinScriptStep
import com.jetbrains.plugin.structure.teamcity.recipe.Steps.someUsesStep
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class ParseValidRecipeTests(
  fileSystemType: FileSystemType,
) : BasePluginManagerTest<TeamCityRecipePlugin, TeamCityRecipePluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path): TeamCityRecipePluginManager =
    TeamCityRecipePluginManager.createManager(extractDirectory)

  @Test
  fun `parse recipe with valid name`() {
    val validRecipeNamesProvider =
      arrayOf("aaaaa/aaaaa", "aaaaa/a-a_a", "a-a_a/aaaaa", "${randomAlphanumeric(30)}/${randomAlphanumeric(30)}")
    validRecipeNamesProvider.forEach { recipeName ->
      Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
      val result = createPluginSuccessfully(temporaryFolder.prepareRecipeYaml(someRecipe.copy(name = recipeName)))
      with(result) {
        assertEquals(recipeName, this.plugin.pluginName)
      }
    }
  }

  @Test
  fun `parse recipe id and namespace`() {
    val expectedNamespace = "jetbrains"
    val expectedRecipeId = "test_recipe"
    val expectedRecipeName = "$expectedNamespace/$expectedRecipeId"

    Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
    val result = createPluginSuccessfully(temporaryFolder.prepareRecipeYaml(someRecipe.copy(name = expectedRecipeName)))
    with(result) {
      assertEquals(expectedRecipeName, this.plugin.pluginName)
      assertEquals(expectedRecipeName, this.plugin.pluginId)
      assertEquals(expectedNamespace, this.plugin.namespace)
    }
  }

  @Test
  fun `parse recipe with command line script step`() {
    val step = someCommandLineScriptStep.copy()
    createPluginSuccessfully(temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(step))))
  }

  @Test
  fun `parse recipe with kotlin script step`() {
    val step = someKotlinScriptStep.copy()
    createPluginSuccessfully(temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(step))))
  }

  @Test
  fun `parse recipe with recipe-based step`() {
    val step = someUsesStep.copy(uses = "recipe/recipeName@1.2.3")
    createPluginSuccessfully(temporaryFolder.prepareRecipeYaml(someRecipe.copy(steps = listOf(step))))
  }

  @Test
  fun `parse recipe when non-archived YAML file is provided`() {
    val yaml = temporaryFolder.newFile("recipe.yaml")
    val recipe = someRecipe.copy()
    Files.writeString(
      yaml,
      ObjectMapper(YAMLFactory()).registerKotlinModule().writeValueAsString(recipe),
    )

    val result = createPluginSuccessfully(yaml)

    with(result.plugin) {
      assertEquals(recipe.name, this.pluginName)
      assertEquals(recipe.version, this.pluginVersion)
    }
  }
}