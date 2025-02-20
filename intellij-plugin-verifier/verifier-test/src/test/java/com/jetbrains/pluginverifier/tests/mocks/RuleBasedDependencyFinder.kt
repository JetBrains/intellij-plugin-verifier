package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder.Result.NotFound
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import org.objectweb.asm.tree.ClassNode

class RuleBasedDependencyFinder(private val ide: Ide, private val rules: List<Rule>): DependencyFinder {

  override val presentableName: String = "Rule-based dependency finder with ${rules.size} rules"

  override fun findPluginDependency(dependencyId: String, isModule: Boolean): DependencyFinder.Result {
    return findRule(dependencyId)
      ?.toDependencyResolution()
      ?: NotFound("Dependency $dependencyId is not found by $presentableName")
  }

  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result =
    findPluginDependency(dependency.id, dependency.isModule)

  private fun findRule(dependencyId: String): Rule? =
    rules.find { it.dependencyId == dependencyId }

  private fun Rule.toDependencyResolution(): DependencyFinder.Result {
    return createLock()
      ?.let { lock -> ResourceCacheEntry(lock) }
      ?.let { cacheEntry -> PluginDetailsCache.Result.Provided(cacheEntry) }
      ?.let { cacheResult ->  DependencyFinder.Result.DetailsProvided(cacheResult) }
      ?: NotFound("Dependency ${plugin.pluginId} is not found by $presentableName")
  }

  private fun Rule.createLock(): MockPluginDetailsProviderLock? {
    return if (isBundledPlugin) {
      MockPluginDetailsProviderLock.ofBundledPlugin(plugin, ide)
    } else {
      MockPluginDetailsProviderLock.of(plugin, toClassesLocations())
    }
  }

  private fun Rule.toClassesLocations(): IdePluginClassesLocations {
    return if (classNodes.isEmpty()) {
      plugin.emptyClassesLocations
    } else {
      bundledPluginClassesLocation(plugin, classNodes)
    }
  }

  companion object {
    fun create(ide: Ide, vararg rules: Rule): DependencyFinder = RuleBasedDependencyFinder(ide, rules.toList())
    fun create(ide: Ide, rules: List<Rule>): DependencyFinder = RuleBasedDependencyFinder(ide, rules)
  }

  data class Rule(val dependencyId: String, val plugin: IdePlugin, val classNodes: List<ClassNode> = emptyList(), val isBundledPlugin: Boolean = false)
}