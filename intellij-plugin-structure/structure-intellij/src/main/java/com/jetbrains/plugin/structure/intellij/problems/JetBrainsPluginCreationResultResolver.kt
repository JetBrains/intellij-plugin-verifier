package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginVendors
import com.jetbrains.plugin.structure.intellij.problems.remapping.JsonUrlProblemLevelRemappingManager
import com.jetbrains.plugin.structure.intellij.problems.remapping.RemappingSet
import java.io.IOException
import kotlin.reflect.KClass


class JetBrainsPluginCreationResultResolver(private val delegatedResolver: PluginCreationResultResolver,
                                            levelRemapping: Map<KClass<*>, RemappedLevel> = emptyMap()
) : PluginCreationResultResolver {

  private val jetBrainsResolver = LevelRemappingPluginCreationResultResolver(delegatedResolver,
    levelRemapping, unwrapRemappedProblems = true)

  override fun resolve(plugin: IdePlugin, problems: List<PluginProblem>): PluginCreationResult<IdePlugin> {
    return getCreationResultResolver(plugin).resolve(plugin, problems)
  }

  override fun classify(plugin: IdePlugin, problems: List<PluginProblem>): List<PluginProblem> {
    return getCreationResultResolver(plugin).classify(plugin, problems)
  }

  private fun getCreationResultResolver(plugin: IdePlugin): PluginCreationResultResolver {
    return if (PluginVendors.isDevelopedByJetBrains(plugin)) {
      jetBrainsResolver
    } else {
      delegatedResolver
    }
  }

  companion object {
    @Throws(IOException::class)
    fun fromClassPathJson(delegatedResolver: PluginCreationResultResolver): JetBrainsPluginCreationResultResolver {
      val levelRemappingManager = JsonUrlProblemLevelRemappingManager.fromClassPathJson()
      val levelRemapping = levelRemappingManager.getLevelRemapping(RemappingSet.JETBRAINS_PLUGIN_REMAPPING_SET)

      return JetBrainsPluginCreationResultResolver(delegatedResolver, levelRemapping)
    }
  }
}
