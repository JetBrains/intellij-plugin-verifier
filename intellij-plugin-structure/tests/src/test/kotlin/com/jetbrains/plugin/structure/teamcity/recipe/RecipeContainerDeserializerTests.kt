package com.jetbrains.plugin.structure.teamcity.recipe

import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Test
import java.nio.file.Path

class RecipeContainerDeserializerTests(
    fileSystemType: FileSystemType,
) : BasePluginManagerTest<TeamCityRecipePlugin, TeamCityRecipePluginManager>(fileSystemType) {

    override fun createManager(extractDirectory: Path): TeamCityRecipePluginManager =
        TeamCityRecipePluginManager.createManager(extractDirectory)

    @Test
    fun `inline recipe container should not cause validation errors`() {
        // arrange
        val recipeYaml = """
            name: namespace/recipe-name
            version: 1.0.0
            description: recipe-description
            container: alpine
            steps:
              - name: step
                script: echo 42
        
        """.trimIndent()
        val pluginFile = temporaryFolder.prepareRecipeYaml(recipeYaml)

        // act, assert
        createPluginSuccessfully(pluginFile)
    }

    @Test
    fun `multiline recipe container should not cause validation errors`() {
        // arrange
        val recipeYaml = """
            name: namespace/recipe-name
            version: 1.0.0
            description: recipe-description
            container:
              image: alpine
              platform: linux
              parameters: -it
            steps:
              - name: step
                script: echo 42
        
        """.trimIndent()
        val pluginFile = temporaryFolder.prepareRecipeYaml(recipeYaml)

        // act, assert
        createPluginSuccessfully(pluginFile)
    }

    @Test
    fun `inline step container should not cause validation errors`() {
        // arrange
        val recipeYaml = """
            name: namespace/recipe-name
            version: 1.0.0
            description: recipe-description
            steps:
              - name: step
                container: alpine
                script: echo 42
        
        """.trimIndent()
        val pluginFile = temporaryFolder.prepareRecipeYaml(recipeYaml)

        // act, assert
        createPluginSuccessfully(pluginFile)
    }

    @Test
    fun `multiline step container should not cause validation errors`() {
        // arrange
        val recipeYaml = """
            name: namespace/recipe-name
            version: 1.0.0
            description: recipe-description
            steps:
              - name: step
                container:
                  image: alpine
                  platform: linux
                  parameters: -it
                script: echo 42
        
        """.trimIndent()
        val pluginFile = temporaryFolder.prepareRecipeYaml(recipeYaml)

        // act, assert
        createPluginSuccessfully(pluginFile)
    }
}