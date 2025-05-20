package com.jetbrains.plugin.structure.teamcity.recipe

import com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.plugin.Settings.EXTRACT_DIRECTORY
import com.jetbrains.plugin.structure.base.problems.FileTooBig
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor
import com.jetbrains.plugin.structure.base.problems.isError
import com.jetbrains.plugin.structure.base.utils.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TeamCityRecipePluginManager
private constructor(private val extractDirectory: Path) : PluginManager<TeamCityRecipePlugin> {

  private val objectMapper = ObjectMapper(
    YAMLFactory()
      .enable(STRICT_DUPLICATE_DETECTION)
  ).registerKotlinModule().apply {
    // We fail on unknown properties to minimize the chance that we improperly calculate the specification version of an uploaded recipe.
    // Since the recipe's properties are used to calculate its specification version,
    // omitting any of the recipe's properties from calculation can lead to an error in calculation.
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
  }

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(TeamCityRecipePluginManager::class.java)

    fun createManager(extractDirectory: Path = Paths.get(EXTRACT_DIRECTORY.get())) =
      TeamCityRecipePluginManager(extractDirectory.createDir())
  }

  override fun createPlugin(pluginFile: Path): PluginCreationResult<TeamCityRecipePlugin> {
    require(pluginFile.exists()) { "TeamCity Recipe file ${pluginFile.toAbsolutePath()} does not exist" }
    return when {
      pluginFile.isYaml() -> createPluginFrom(pluginFile)
      else -> PluginCreationFail(NotYamlFileProblem)
    }
  }

  private fun createPluginFrom(recipePath: Path): PluginCreationResult<TeamCityRecipePlugin> {
    val sizeLimit = Settings.TEAM_CITY_RECIPE_SIZE_LIMIT.getAsLong()
    if (Files.size(recipePath) > sizeLimit) {
      return PluginCreationFail(FileTooBig(recipePath.simpleName, sizeLimit))
    }

    return when {
      recipePath.isYaml() -> parseYaml(recipePath)
      else -> PluginCreationFail(NotYamlFileProblem)
    }
  }

  private fun parseYaml(yamlPath: Path): PluginCreationResult<TeamCityRecipePlugin> {
    try {
      return parse(yamlPath)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      val errorMessage = "An unexpected error occurred while parsing the TeamCity Recipe descriptor"
      LOG.warn(errorMessage, e)
      return PluginCreationFail(UnableToReadDescriptor(yamlPath.toAbsolutePath().toString(), errorMessage))
    }
  }

  private fun parse(yamlPath: Path): PluginCreationResult<TeamCityRecipePlugin> {
    val descriptor = try {
      val yamlContent = yamlPath.readText()
      objectMapper.readValue(yamlContent, TeamCityRecipeDescriptor::class.java)
    } catch (e: UnrecognizedPropertyException) {
      LOG.warn("Failed to parse TeamCity Recipe. Encountered unknown property '${e.propertyName}'", e)
      return PluginCreationFail(UnknownPropertyProblem(e.propertyName))
    } catch (e: Exception) {
      LOG.warn("Failed to parse TeamCity Recipe", e)

      if (e.message?.startsWith("Duplicate field") == true) {
        return PluginCreationFail(DuplicatePropertiesProblem)
      }

      return PluginCreationFail(ParseYamlProblem)
    }

    val validationResult = validateTeamCityRecipe(descriptor)
    if (validationResult.any { it.isError }) {
      return PluginCreationFail(validationResult)
    }
    val plugin = with(descriptor) {
      TeamCityRecipePlugin(
        // All the fields are expected to be non-null due to the validations above
        pluginId = this.name!!, // composite id
        pluginName = this.title!!,
        description = this.description!!,
        pluginVersion = this.version!!,
        specVersion = getSpecVersion(),
        yamlFile = PluginFile(yamlPath.fileName.toString(), yamlPath.readBytes()),
        namespace = TeamCityRecipeSpec.RecipeCompositeName.getNamespace(this.name)!!,
      )
    }
    return PluginCreationSuccess(plugin, validationResult)
  }
}

// Always 1.0.0 for now
private fun getSpecVersion(): String = "1.0.0"

private fun Path.isYaml(): Boolean = this.hasExtension("yaml") || this.hasExtension("yml")