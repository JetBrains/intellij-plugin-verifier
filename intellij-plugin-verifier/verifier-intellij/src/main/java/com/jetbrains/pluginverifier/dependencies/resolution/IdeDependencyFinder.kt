package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginRepository

fun createIdeBundledOrPluginRepositoryDependencyFinder(
  ide: Ide,
  pluginRepository: PluginRepository,
  pluginDetailsCache: PluginDetailsCache
): DependencyFinder {
  val bundledPluginFinder = BundledPluginDependencyFinder(ide, pluginDetailsCache)

  val repositoryDependencyFinder = RepositoryDependencyFinder(
    pluginRepository,
    LastCompatibleVersionSelector(ide.version),
    pluginDetailsCache
  )

  return CompositeDependencyFinder(listOf(bundledPluginFinder, repositoryDependencyFinder))
}