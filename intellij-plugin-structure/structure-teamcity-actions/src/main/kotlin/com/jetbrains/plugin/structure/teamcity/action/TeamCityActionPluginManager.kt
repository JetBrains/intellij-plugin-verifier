package com.jetbrains.plugin.structure.teamcity.action

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionStepWith.ACTION_PREFIX
import com.jetbrains.plugin.structure.teamcity.action.TeamCityActionSpec.ActionStepWith.RUNNER_PREFIX
import com.jetbrains.plugin.structure.teamcity.action.model.*
import com.jetbrains.plugin.structure.teamcity.action.problems.ParseYamlProblem
import com.vdurmont.semver4j.Semver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.nio.file.Paths

class TeamCityActionPluginManager private constructor(
  private val extractDirectory: Path,
) : PluginManager<TeamCityActionPlugin> {

  private val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(TeamCityActionPluginManager::class.java)

    fun createManager(
      extractDirectory: Path = Paths.get(Settings.EXTRACT_DIRECTORY.get()),
    ): TeamCityActionPluginManager {
      extractDirectory.createDir()
      return TeamCityActionPluginManager(extractDirectory)
    }
  }

  override fun createPlugin(pluginFile: Path): PluginCreationResult<TeamCityActionPlugin> {
    require(pluginFile.exists()) { "TeamCity Action file ${pluginFile.toAbsolutePath()} does not exist" }
    return when {
      pluginFile.isZip() || pluginFile.isYaml() -> parse(pluginFile)
      else -> fileFormatError(pluginFile)
    }
  }

  private fun parse(actionPath: Path): PluginCreationResult<TeamCityActionPlugin> {
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
    val yamlContent = yamlPath.readText()
    val actionDescriptor: TeamCityActionDescriptor
    try {
      actionDescriptor = objectMapper.readValue(yamlContent, TeamCityActionDescriptor::class.java)
    } catch (e: Exception) {
      LOG.warn("Failed to parse TeamCity Action. Error: ${e.message}", e)
      return PluginCreationFail(ParseYamlProblem)
    }
    try {
      return parse(actionDescriptor)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.warn("Unable to read TeamCity Action descriptor", e)
      return PluginCreationFail(UnableToReadDescriptor(yamlPath.toAbsolutePath().toString(), e.localizedMessage))
    }
  }

  private fun parse(descriptor: TeamCityActionDescriptor): PluginCreationResult<TeamCityActionPlugin> {
    val validationResult = validateTeamCityAction(descriptor)
    if (validationResult.any { it.isError }) {
      return PluginCreationFail(validationResult)
    }
    val plugin = with(descriptor) {
      TeamCityActionPlugin(
        pluginId = this.name + "@" + this.version, // TODO: It is a temporary way of the pluginID creation
        pluginName = this.name!!,
        description = this.description!!,
        pluginVersion = this.version!!,
        specVersion = Semver(this.specVersion!!),
        inputs = inputs(descriptor.inputs),
        requirements = requirements(descriptor.requirements),
        steps = steps(descriptor.steps),
      )
    }
    return PluginCreationSuccess(plugin, validationResult)
  }
}

private fun inputs(inputs: List<Map<String, ActionInputDescriptor>>) =
  inputs.asSequence().map(::input).toList()

private fun input(input: Map<String, ActionInputDescriptor>): ActionInput {
  val inputName = input.keys.first()
  val inputValue = input.values.first()
  return when (inputValue.type!!) {
    ActionInputTypeDescriptor.text.name -> TextActionInput(
      inputName,
      inputValue.isRequired.toBoolean(),
      inputValue.label,
      inputValue.description,
      inputValue.defaultValue,
    )

    ActionInputTypeDescriptor.boolean.name -> BooleanActionInput(
      inputName,
      inputValue.isRequired.toBoolean(),
      inputValue.label,
      inputValue.description,
      inputValue.defaultValue,
    )

    ActionInputTypeDescriptor.select.name -> SelectActionInput(
      inputName,
      inputValue.isRequired.toBoolean(),
      inputValue.label,
      inputValue.description,
      inputValue.defaultValue,
      inputValue.selectOptions,
    )

    else -> throw IllegalArgumentException(
      "Unsupported action input type: ${inputValue.type}. " +
              "Supported values are: ${ActionInputTypeDescriptor.values().joinToString()}"
    )
  }
}

private fun requirements(requirements: List<Map<String, ActionRequirementDescriptor>>) =
  requirements.asSequence().map(::requirement).toList()

private fun requirement(requirement: Map<String, ActionRequirementDescriptor>): ActionRequirement {
  val requirementName = requirement.keys.first()
  val requirementValue = requirement.values.first()
  return ActionRequirement(
    requirementName,
    ActionRequirementType.from(requirementValue.type!!),
    requirementValue.value
  )
}

private fun steps(steps: List<ActionStepDescriptor>) =
  steps.asSequence().map(::step).toList()

private fun step(step: ActionStepDescriptor): ActionStep {
  if (step.script != null) {
    return RunnerBasedStep(
      step.name!!,
      mapOf("script.content" to step.script),
      "simpleRunner",
    )
  }
  if (step.with!!.startsWith(RUNNER_PREFIX)) {
    val runnerName = step.with.removePrefix(RUNNER_PREFIX)
    return RunnerBasedStep(
      step.name!!,
      step.parameters,
      runnerName,
    )
  } else if (step.with.startsWith(ACTION_PREFIX)) {
    val actionId = step.with.removePrefix(ACTION_PREFIX)
    return ActionBasedStep(
      step.name!!,
      step.parameters,
      actionId,
    )
  }
  throw IllegalArgumentException("Failed to parse action step")
}

private fun fileFormatError(pluginFile: Path): PluginCreationResult<TeamCityActionPlugin> =
  PluginCreationFail(IncorrectPluginFile(pluginFile.simpleName, ".zip archive or .yaml file"))

private fun Path.isYaml(): Boolean = this.hasExtension("yaml") || this.hasExtension("yml")