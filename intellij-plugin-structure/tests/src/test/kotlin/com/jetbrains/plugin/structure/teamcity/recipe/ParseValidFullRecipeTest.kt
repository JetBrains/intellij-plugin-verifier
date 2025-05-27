package com.jetbrains.plugin.structure.teamcity.recipe

import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path

class ParseValidFullRecipeTest(
  fileSystemType: FileSystemType,
) : BasePluginManagerTest<TeamCityRecipePlugin, TeamCityRecipePluginManager>(fileSystemType) {

  override fun createManager(extractDirectory: Path): TeamCityRecipePluginManager =
    TeamCityRecipePluginManager.createManager(extractDirectory)

  private val recipeYaml =
    """
    ---
    name: namespace/simple-recipe
    version: 1.2.3
    title: Simple recipe
    description: this is a simple recipe
    container:
      image: alpine
      platform: linux
      parameters: -it
    inputs:
      - some text input:
          type: text
          label: text input
          default: a default text value
          required: false
          description: a description for the text input
      - some boolean input:
          type: boolean
          default: true
      - some select input:
          type: select
          label: select input
          options:
            - first select option
            - second select option
          default: first select option
          required: true
          description: description for select input
      - some password input:
          type: password
    steps:
      - name: step 1
        script: echo "step 2 output"
        container:
          image: alpine
          platform: linux
          parameters: -it
      - name: step 2
        kotlin-script: print("hi")
      - name: step 3
        uses: jetbrains/recipe@1.2.3
      - name: step 4
        uses: namespace/name@1.0.0
        inputs:
          text-input: passed text parameter value
          boolean-input: true
          select-input: first select option
          password-input: asdad
    """.trimIndent()

  @Test
  fun `parse full valid TeamCity Recipe from YAML`() {
    // arrange
    val pluginFile = temporaryFolder.prepareRecipeYaml(recipeYaml)

    // act
    val result = createPluginSuccessfully(pluginFile)

    // assert
    with(result.plugin) {
      assertEquals("recipe.yaml", this.yamlFile.fileName)
      assertArrayEquals(recipeYaml.toByteArray(), this.yamlFile.content)
      assertEquals("1.0.0", this.specVersion)
      assertEquals("Simple recipe", this.pluginName)
      assertEquals("namespace", this.namespace)
      assertEquals("1.2.3", this.pluginVersion)
      assertEquals("this is a simple recipe", this.description)
      assertEquals(2, this.dependencies.size)
      assertEquals(true, this.dependencies.contains(TeamCityRecipeDependency("jetbrains", "recipe", "1.2.3")))
      assertEquals(true, this.dependencies.contains(TeamCityRecipeDependency("namespace", "name", "1.0.0")))
    }
  }
}