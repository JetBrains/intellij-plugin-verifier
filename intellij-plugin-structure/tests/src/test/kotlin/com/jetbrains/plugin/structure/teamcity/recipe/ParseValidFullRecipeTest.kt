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
    description: this is a simple recipe
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
      - some number input:
          type: number
          default: 100
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
          default: qwerty
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
          pom-location: pom.xml
          goals: build
      - script: echo "step 2 output"
        name: step 2
      - name: step 3
        with: recipe/name@1.2.3
        params:
          text-input: passed text parameter value
          boolean-input: true
          number-input: 123
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
      assertEquals("namespace/simple-recipe", this.pluginName)
      assertEquals("namespace", this.namespace)
      assertEquals("1.2.3", this.pluginVersion)
      assertEquals("this is a simple recipe", this.description)
    }
  }
}