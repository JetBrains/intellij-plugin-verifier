package com.jetbrains.plugin.structure.teamcity.recipe

import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class RecipeContainerDeserializerNegativeTests(
    fileSystemType: FileSystemType,
) : BasePluginManagerTest<TeamCityRecipePlugin, TeamCityRecipePluginManager>(fileSystemType) {

    override fun createManager(extractDirectory: Path): TeamCityRecipePluginManager =
        TeamCityRecipePluginManager.createManager(extractDirectory)

    @Test
    fun `validate recipe image`() {
        val recipeImageProvider = arrayOf(
            // empty recipe container
            """
        container: {}
        steps:
          - name: step_1
            script: echo "kek"
        """.trimIndent(),
            // empty step container
            """
        steps:
          - name: step_1
            script: echo "kek"
            container: {}
        """.trimIndent(),
            // recipe container with empty image
            """
        container:
          image:
        steps:
          - name: step_1
            script: echo "kek"
        """.trimIndent(),
            // step container with empty image
            """
        steps:
          - name: step_1
            script: echo "kek"
            container:
              image:
        """.trimIndent(),
            // recipe container without image but with platform
            """
        container:
          platform: linux
        steps:
          - name: step_1
            script: echo "kek"
        """.trimIndent(),
            // step container without image but with platform
            """
        steps:
          - name: step_1
            script: echo "kek"
            container:
              platform: linux
        """.trimIndent(),
            // recipe container without image but with additional docker parameters
            """
        container:
          parameters: --pull=always
        steps:
          - name: step_1
            script: echo "kek"
        """.trimIndent(),
            // step container without image but with additional docker parameters
            """
        steps:
          - name: step_1
            script: echo "kek"
            container:
              parameters: --pull=always
        """.trimIndent()
        )
        recipeImageProvider.forEach { recipeBody ->
            Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
            assertProblematicPlugin(
                temporaryFolder.prepareRecipeYaml(prepareFullRecipeYaml(recipeBody)),
                listOf(MissingValueProblem("image", "container image name")),
            )
        }
    }

    @Test
    fun `validate recipe with unsupported docker platform`() {
        val unsupportedPlatformsProvider = arrayOf(
            // recipe container
            """
        container:
          image: alpine
          platform: unsupported
        steps:
          - name: step_1
            script: echo "kek"
        """.trimIndent(),
            // step container
            """
        steps:
          - name: step_1
            script: echo "kek"
            container:
              image: alpine
              platform: unsupported
        """.trimIndent()
        )
        unsupportedPlatformsProvider.forEach { recipeBody ->
            Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
            assertProblematicPlugin(
                temporaryFolder.prepareRecipeYaml(prepareFullRecipeYaml(recipeBody)),
                listOf(InvalidPropertyValueProblem("Wrong recipe container image platform: unsupported. Supported values are: Linux, Windows.")),
            )
        }
    }

    @Test
    fun `validate recipe with empty inline image`() {
        val emptyInlineContainersProvider = arrayOf(
            // recipe container
            """
        container:
          image: ""
        steps:
          - name: step_1
            script: echo "kek"
        """.trimIndent(),
            // step container
            """
        steps:
          - name: step_1
            script: echo "kek"
            container:
              image: ""
        """.trimIndent()
        )
        emptyInlineContainersProvider.forEach { recipeBody ->
            Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
            assertProblematicPlugin(
                temporaryFolder.prepareRecipeYaml(prepareFullRecipeYaml(recipeBody)),
                listOf(EmptyValueProblem("image", "container image name")),
            )
        }
    }

    @Test
    fun `validate recipe with empty container parameters`() {
        val emptyContainerParametersProvider = arrayOf(
            // recipe container
            """
        container:
          image: alpine
          parameters: ""
        steps:
          - name: step_1
            script: echo "kek"
        """.trimIndent(),
            // step container
            """
        steps:
          - name: step_1
            script: echo "kek"
            container:
              image: alpine
              parameters: ""
        """.trimIndent()
        )
        emptyContainerParametersProvider.forEach { recipeBody ->
            Files.walk(temporaryFolder.root).filter { it.isFile }.forEach { Files.delete(it) }
            assertProblematicPlugin(
                temporaryFolder.prepareRecipeYaml(prepareFullRecipeYaml(recipeBody)),
                listOf(EmptyValueProblem("parameters", "container run options")),
            )
        }
    }

    private fun prepareFullRecipeYaml(recipeBody: String): String {
        val recipeHeader = """
        name: namespace/recipe-name
        version: 1.0.0
        version: 1.2.3
        description: abc
        
        """.trimIndent()
        return recipeHeader + recipeBody;
    }
}