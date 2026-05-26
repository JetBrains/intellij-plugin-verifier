/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.misc.retry
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.dependency.DependencyPluginRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(RepositoryDependencyFinder::class.java)

/**
 * [DependencyFinder] that searches for the dependency in the [PluginRepository].
 * The [pluginVersionSelector] is used to select a specific version of the plugin
 * if multiple versions are available.
 */
class RepositoryDependencyFinder(
  pluginRepository: PluginRepository,
  private val pluginVersionSelector: PluginVersionSelector,
  private val pluginDetailsCache: PluginDetailsCache
) : DependencyFinder {

  private val pluginRepository = DependencyPluginRepository(pluginRepository)

  override val presentableName
    get() = pluginRepository.toString()

  override fun findPluginDependency(dependencyId: String, isModule: Boolean): DependencyFinder.Result =
    retry("Resolve dependency $dependencyId") {
      if (dependencyId.isBlank()) {
        return@retry DependencyFinder.Result.NotFound("Invalid empty dependency ID")
      }
      val kind = if (isModule) "module" else "plugin"
      LOG.debug("Resolving dependency {} '{}' against {}", kind, dependencyId, pluginRepository)
      val selectResult = if (isModule) {
        pluginVersionSelector.selectPluginByModuleId(dependencyId, pluginRepository)
      } else {
        pluginVersionSelector.selectPluginVersion(dependencyId, pluginRepository)
      }
      convertResult(dependencyId, kind, selectResult)
    }

  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result =
    findPluginDependency(dependency.id, dependency.isModule)

  private fun convertResult(
    dependencyId: String,
    kind: String,
    selectResult: PluginVersionSelector.Result
  ): DependencyFinder.Result =
    when (selectResult) {
      is PluginVersionSelector.Result.Selected -> {
        LOG.info(
          "Resolved dependency {} '{}' to {} against {}",
          kind,
          dependencyId,
          selectResult.pluginInfo,
          pluginRepository
        )
        val cacheEntryResult = pluginDetailsCache.getPluginDetailsCacheEntry(selectResult.pluginInfo)
        DependencyFinder.Result.DetailsProvided(cacheEntryResult)
      }
      is PluginVersionSelector.Result.NotFound -> {
        LOG.debug(
          "Dependency {} '{}' not found in {}: {}",
          kind,
          dependencyId,
          pluginRepository,
          selectResult.reason
        )
        DependencyFinder.Result.NotFound(selectResult.reason)
      }
    }
}