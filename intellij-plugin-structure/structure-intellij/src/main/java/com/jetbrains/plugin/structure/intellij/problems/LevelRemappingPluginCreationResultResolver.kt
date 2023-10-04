package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.ReclassifiedPluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import kotlin.reflect.KClass

class LevelRemappingPluginCreationResultResolver(private val delegatedResolver: PluginCreationResultResolver,
                                                 private val additionalLevelRemapping: Map<KClass<*>, PluginProblem.Level> = emptyMap()
  ) : PluginCreationResultResolver {

  private val remappedLevel: Map<KClass<*>, PluginProblem.Level> = additionalLevelRemapping +
    mapOf(IllegalPluginIdPrefix::class to PluginProblem.Level.WARNING)

  override fun resolve(plugin: IdePlugin, problems: List<PluginProblem>): PluginCreationResult<IdePlugin> {
    return when (val pluginCreationResult = delegatedResolver.resolve(plugin, problems)) {
      is PluginCreationSuccess -> remapSuccess(pluginCreationResult)
      is PluginCreationFail -> remapFailure(plugin, pluginCreationResult)
    }
  }

  private fun remapSuccess(pluginCreationResult: PluginCreationSuccess<IdePlugin>): PluginCreationResult<IdePlugin> {
    return with(pluginCreationResult) {
      copy(warnings = remapWarnings(warnings), unacceptableWarnings = remapUnacceptableWarnings(unacceptableWarnings))
    }
  }

  private fun remapFailure(plugin: IdePlugin, pluginCreationResult: PluginCreationFail<IdePlugin>): PluginCreationResult<IdePlugin> {
    return with(pluginCreationResult) {
      val remappedErrorsAndWarnings = remapErrorsAndWarnings(errorsAndWarnings)
      if (remappedErrorsAndWarnings.hasNoErrors()) {
        return PluginCreationSuccess(plugin, remappedErrorsAndWarnings)
      } else {
        copy(errorsAndWarnings = remapErrorsAndWarnings(this.errorsAndWarnings))
      }
    }
  }

  private fun remapWarnings(warnings: List<PluginProblem>): List<PluginProblem> {
    return warnings.map(::remapPluginProblemLevel)
  }

  private fun remapUnacceptableWarnings(unacceptableWarnings: List<PluginProblem>): List<PluginProblem> {
    return unacceptableWarnings.map(::remapPluginProblemLevel)
  }

  private fun remapErrorsAndWarnings(errorsAndWarnings: List<PluginProblem>): List<PluginProblem> {
    return errorsAndWarnings.map(::remapPluginProblemLevel)
  }

  private fun remapPluginProblemLevel(pluginProblem: PluginProblem): PluginProblem {
    val remappedLevel = remappedLevel[pluginProblem::class]
    if (remappedLevel != null) {
      return ReclassifiedPluginProblem(remappedLevel, pluginProblem)
    }
    return pluginProblem
  }

  override fun classify(plugin: IdePlugin, problem: PluginProblem): PluginProblem {
    return remapPluginProblemLevel(problem)
  }

  private fun List<PluginProblem>.hasNoErrors(): Boolean = none {
    it.level == PluginProblem.Level.ERROR
  }
}