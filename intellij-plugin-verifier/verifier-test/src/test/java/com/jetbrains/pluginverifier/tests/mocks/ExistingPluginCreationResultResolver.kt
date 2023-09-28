package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.problems.DelegatingPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.IllegalPluginIdPrefix
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

class ExistingPluginCreationResultResolver(delegatedResolver: PluginCreationResultResolver) : DelegatingPluginCreationResultResolver(delegatedResolver) {
  private val logger = LoggerFactory.getLogger("verification.structure")

  private val existingPluginAllowedProblems: List<KClass<*>> = listOf(IllegalPluginIdPrefix::class)

  override fun doResolve(plugin: IdePlugin, problems: List<PluginProblem>): ResolutionResult {
    val filteredProblems = problems.filterNot {
      val isAllowed = existingPluginAllowedProblems.contains(it::class)
      if (isAllowed) {
        logger.info("Plugin problem will be ignored for existing plugins: $it")
      }
      isAllowed
    }
    return ResolutionResult.Resolved(delegatedResolver.resolve(plugin, filteredProblems))
  }
}