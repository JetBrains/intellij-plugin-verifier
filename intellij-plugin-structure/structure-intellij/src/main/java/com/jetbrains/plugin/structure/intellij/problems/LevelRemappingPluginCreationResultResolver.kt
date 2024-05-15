package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.UNACCEPTABLE_WARNING
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.WARNING
import com.jetbrains.plugin.structure.base.problems.ReclassifiedPluginProblem
import com.jetbrains.plugin.structure.base.problems.unwrapped
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import kotlin.reflect.KClass

class LevelRemappingPluginCreationResultResolver(private val delegatedResolver: PluginCreationResultResolver,
                                                 private val remappedLevel: Map<KClass<*>, RemappedLevel> = emptyMap(),
                                                 private val unwrapRemappedProblems: Boolean = false
  ) : PluginCreationResultResolver {

  override fun resolve(plugin: IdePlugin, problems: List<PluginProblem>): PluginCreationResult<IdePlugin> {
    return when (val pluginCreationResult = delegatedResolver.resolve(plugin, problems)) {
      is PluginCreationSuccess -> remapSuccess(pluginCreationResult)
      is PluginCreationFail -> remapFailure(plugin, pluginCreationResult)
    }
  }

  private fun remapSuccess(pluginCreationResult: PluginCreationSuccess<IdePlugin>): PluginCreationResult<IdePlugin> {
    return with(pluginCreationResult) {
      val allRemappedProblems = remapWarnings(warnings) + remapUnacceptableWarnings(unacceptableWarnings)
      if (allRemappedProblems.hasNoErrors()) {
        copy(warnings = allRemappedProblems.warnings(), unacceptableWarnings = allRemappedProblems.unacceptableWarnings())
      } else {
        PluginCreationFail(allRemappedProblems)
      }
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
    return warnings.mapNotNull(::remapPluginProblemLevel)
  }

  private fun remapUnacceptableWarnings(unacceptableWarnings: List<PluginProblem>): List<PluginProblem> {
    return unacceptableWarnings.mapNotNull(::remapPluginProblemLevel)
  }

  private fun remapErrorsAndWarnings(errorsAndWarnings: List<PluginProblem>): List<PluginProblem> {
    return errorsAndWarnings.mapNotNull(::remapPluginProblemLevel)
  }

  private fun remapPluginProblemLevel(pluginProblem: PluginProblem): PluginProblem? {
    return remapPluginProblemLevel(pluginProblem, unwrapRemappedProblems)
  }

  private fun remapPluginProblemLevel(pluginProblem: PluginProblem, unwrapRemappedProblems: Boolean): PluginProblem? {
    val problem = if (unwrapRemappedProblems) {
      pluginProblem.unwrapped
    } else {
      pluginProblem
    }

    return when (val remappedLevel = remappedLevel[problem::class]) {
      is StandardLevel -> ReclassifiedPluginProblem(remappedLevel.originalLevel, pluginProblem)
      is IgnoredLevel -> null
      null -> pluginProblem
    }
  }

  override fun classify(plugin: IdePlugin, problems: List<PluginProblem>): List<PluginProblem> {
    return delegatedResolver.classify(plugin, problems).mapNotNull {
      classify(it.unwrapped)
    }
  }

  private fun classify(pluginProblem: PluginProblem): PluginProblem? {
    return when (val remappedLevel = remappedLevel[pluginProblem::class]) {
      is StandardLevel -> ReclassifiedPluginProblem(remappedLevel.originalLevel, pluginProblem)
      is IgnoredLevel -> null
      null -> pluginProblem
    }
  }

  private fun List<PluginProblem>.hasNoErrors(): Boolean = none {
    it.level == PluginProblem.Level.ERROR
  }

  private fun List<PluginProblem>.warnings() = filter { it.level == WARNING }
  private fun List<PluginProblem>.unacceptableWarnings() = filter { it.level == UNACCEPTABLE_WARNING }
}