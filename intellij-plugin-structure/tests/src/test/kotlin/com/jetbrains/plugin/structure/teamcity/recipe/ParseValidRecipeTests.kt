package com.jetbrains.plugin.structure.teamcity.recipe

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
  fun `parse recipe name and namespace`() {
    val validRecipeNamesProvider =
      arrayOf("jetbrains/test_recipe", "aaaaa/aaaaa", "aaaaa/a-a_a", "a-a_a/aaaaa", "${randomAlphanumeric(30)}/${randomAlphanumeric(30)}")
    validRecipeNamesProvider.forEach { recipeName ->
      Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
      val result = createPluginSuccessfully(temporaryFolder.prepareRecipeYaml(someRecipe.copy(name = recipeName)))
      with(result.plugin) {
        assertEquals(recipeName, this.pluginId)
        assertEquals(recipeName.substringBefore('/'), this.namespace)
      }
    }
  }

  @Test
  fun `parse recipe title`() {
    val expectedRecipeTitle = "a recipe title"

    Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
    val result = createPluginSuccessfully(temporaryFolder.prepareRecipeYaml(someRecipe.copy(title = expectedRecipeTitle)))
    with(result.plugin) {
      assertEquals(expectedRecipeTitle, this.pluginName)
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
}