package com.jetbrains.plugin.structure.teamcity.action

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.plugin.Settings.EXTRACT_DIRECTORY
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.nio.file.Paths

class TeamCityActionPluginManager
private constructor(private val extractDirectory: Path) : PluginManager<TeamCityActionPlugin> {

  private val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(TeamCityActionPluginManager::class.java)

    fun createManager(extractDirectory: Path = Paths.get(EXTRACT_DIRECTORY.get())) =
      TeamCityActionPluginManager(extractDirectory.createDir())
  }

  override fun createPlugin(pluginFile: Path): PluginCreationResult<TeamCityActionPlugin> {
    require(pluginFile.exists()) { "TeamCity Action file ${pluginFile.toAbsolutePath()} does not exist" }
    return when {
      pluginFile.isZip() || pluginFile.isYaml() -> createPluginFrom(pluginFile)
      else -> fileFormatError(pluginFile)
    }
  }

  private fun createPluginFrom(actionPath: Path): PluginCreationResult<TeamCityActionPlugin> {
    val sizeLimit = Settings.TEAM_CITY_ACTION_SIZE_LIMIT.getAsLong()
    if (Files.size(actionPath) > sizeLimit) {
      return PluginCreationFail(FileTooBig(actionPath.simpleName, sizeLimit))
    }

    return when {
      actionPath.isZip() -> extractActionFromZip(actionPath, sizeLimit)
      actionPath.isYaml() -> parseYaml(actionPath)
      else -> fileFormatError(actionPath)
    }
  }

  private fun extractActionFromZip(zipPath: Path, sizeLimit: Long): PluginCreationResult<TeamCityActionPlugin> {
    val tempDirectory = createTempDirectory(extractDirectory, "teamcity_action_extracted_${zipPath.simpleName}_")
    return try {
      extractZip(zipPath, tempDirectory, sizeLimit)
      val yaml = Files.walk(tempDirectory)
        .filter { item -> item.isYaml() }
        .findFirst()
      if (yaml.isEmpty) {
        return PluginCreationFail(MissedFile("action.yaml"))
      }
      parseYaml(yaml.get())
    } catch (e: DecompressorSizeLimitExceededException) {
      return PluginCreationFail(PluginFileSizeIsTooLarge(e.sizeLimit))
    } finally {
      tempDirectory.deleteLogged()
    }
  }

  private fun parseYaml(yamlPath: Path): PluginCreationResult<TeamCityActionPlugin> {
    try {
      return parse(yamlPath)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      val errorMessage = "An unexpected error occurred while parsing the TeamCity Action descriptor"
      LOG.warn(errorMessage, e)
      return PluginCreationFail(UnableToReadDescriptor(yamlPath.toAbsolutePath().toString(), errorMessage))
    }
  }

  private fun parse(yamlPath: Path): PluginCreationResult<TeamCityActionPlugin> {
    val descriptor = try {
      val yamlContent = yamlPath.readText()
      objectMapper.readValue(yamlContent, TeamCityActionDescriptor::class.java)
    } catch (e: Exception) {
      LOG.warn("Failed to parse TeamCity Action", e)
      return PluginCreationFail(ParseYamlProblem)
    }

    val validationResult = validateTeamCityAction(descriptor)
    if (validationResult.any { it.isError }) {
      return PluginCreationFail(validationResult)
    }
    val plugin = with(descriptor) {
      TeamCityActionPlugin(
        // All the fields are expected to be non-null due to the validations above
        pluginId = this.name!!, // composite id
        pluginName = this.name,
        description = this.description!!,
        pluginVersion = this.version!!,
        specVersion = this.specVersion!!,
        yamlFile = PluginFile(yamlPath.fileName.toString(), yamlPath.readBytes()),
        namespace = TeamCityActionSpec.ActionCompositeName.getNamespace(this.name)!!
      )
    }
    return PluginCreationSuccess(plugin, validationResult)
  }
}

private fun fileFormatError(pluginFile: Path): PluginCreationResult<TeamCityActionPlugin> =
  PluginCreationFail(IncorrectPluginFile(pluginFile.simpleName, "ZIP archive or YAML file"))

private fun Path.isYaml(): Boolean = this.hasExtension("yaml") || this.hasExtension("yml")