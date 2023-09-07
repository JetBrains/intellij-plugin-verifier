package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

abstract class DelegatingPluginCreationResultResolver(protected val delegatedResolver: PluginCreationResultResolver) : PluginCreationResultResolver {
  override fun resolve(plugin: IdePlugin, problems: List<PluginProblem>): PluginCreationResult<IdePlugin> {
    return when (val resolutionResult = doResolve(plugin, problems)) {
      is ResolutionResult.Resolved -> resolutionResult.result
      is ResolutionResult.Delegated -> delegatedResolver.resolve(plugin, problems)
    }
  }

  abstract fun doResolve(plugin: IdePlugin, problems: List<PluginProblem>): ResolutionResult

  sealed class ResolutionResult {
    class Resolved(val result: PluginCreationResult<IdePlugin>) : ResolutionResult()

    class Delegated(val plugin: IdePlugin, val problems: List<PluginProblem>) : ResolutionResult()
  }

}

