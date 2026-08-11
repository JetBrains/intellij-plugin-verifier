/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.pluginverifier.dependencies.resolution.*
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginRepository
import org.jetbrains.annotations.ApiStatus

/**
 * Creates [DependencyFinder] that searches dependencies using the following order:
 * 1) Bundled with [releaseOrTrunkIde]
 * 2) Available in the local repository [localPluginRepository].
 * 3) Compatible with the **release** IDE
 */
@ApiStatus.Internal
fun createDependencyFinder(
  releaseOrTrunkIde: Ide,
  releaseIde: Ide,
  pluginRepository: PluginRepository,
  localPluginRepository: PluginRepository,
  pluginDetailsCache: PluginDetailsCache
): DependencyFinder {
  val bundledFinder = BundledPluginDependencyFinder(releaseOrTrunkIde)

  val localRepositoryDependencyFinder = RepositoryDependencyFinder(
    localPluginRepository,
    LastVersionSelector(),
    pluginDetailsCache
  )

  val releaseDependencyFinder = RepositoryDependencyFinder(
    pluginRepository,
    LastCompatibleVersionSelector(releaseIde.version),
    pluginDetailsCache
  )

  return CompositeDependencyFinder(
    listOf(
      bundledFinder,
      localRepositoryDependencyFinder,
      releaseDependencyFinder
    )
  )
}

