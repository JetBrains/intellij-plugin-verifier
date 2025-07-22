package com.jetbrains.plugin.structure.teamcity.recipe

import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path

class RecipeSpecVersionTests(
    fileSystemType: FileSystemType,
) : BasePluginManagerTest<TeamCityRecipePlugin, TeamCityRecipePluginManager>(fileSystemType) {

    override fun createManager(extractDirectory: Path): TeamCityRecipePluginManager =
        TeamCityRecipePluginManager.createManager(extractDirectory)

    @Test
    fun `should return 1_0_0 version by default`() {
        // arrange
        val recipeYaml = """
            name: namespace/recipe-name
            version: 1.0.0
            title: title
            description: recipe-description
            steps:
              - name: step
                script: echo hi
        
        """.trimIndent()
        val pluginFile = temporaryFolder.prepareRecipeYaml(recipeYaml)

        // act
        val result = createPluginSuccessfully(pluginFile)

        // assert
        with(result.plugin) {
            assertEquals("1.0.0", this.specVersion)
        }
    }

    @Test
    fun `should return 1_1_0 version if path prefix variable is used`() {
        // arrange
        val recipeYaml = """
            name: namespace/recipe-name
            version: 1.0.0
            title: title
            description: recipe-description
            steps:
              - name: step
                kotlin-script: echo System.getenv("TEAMCITY_PATH_PREFIX")
        
        """.trimIndent()
        val pluginFile = temporaryFolder.prepareRecipeYaml(recipeYaml)

        // act
        val result = createPluginSuccessfully(pluginFile)

        // assert
        with(result.plugin) {
            assertEquals("1.1.0", this.specVersion)
        }
    }
}