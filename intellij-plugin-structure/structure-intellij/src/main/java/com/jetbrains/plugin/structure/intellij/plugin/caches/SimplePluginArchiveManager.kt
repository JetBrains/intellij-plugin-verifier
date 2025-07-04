/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.caches

import com.jetbrains.plugin.structure.base.utils.Deletable
import com.jetbrains.plugin.structure.intellij.resources.PluginArchiveResource
import java.io.Closeable
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class SimplePluginArchiveManager : PluginArchiveManager, Deletable, Closeable {
  private val cache = ConcurrentHashMap.newKeySet<PluginArchiveResource>()

  override fun getPluginResource(pluginArtifactPath: Path): PluginArchiveManager.Result {
    return cache.find { it.artifactPath == pluginArtifactPath }
      ?.let { PluginArchiveManager.Result.Found(it) }
      ?: PluginArchiveManager.Result.NotFound
  }

  override fun findFirst(predicate: (PluginArchiveResource) -> Boolean): PluginArchiveManager.Result {
    return cache.find(predicate)
      ?.let { PluginArchiveManager.Result.Found(it) }
      ?: PluginArchiveManager.Result.NotFound
  }

  override fun plusAssign(pluginResource: PluginArchiveResource) {
    cache += pluginResource
  }

  @Synchronized
  fun clear() {
    cache.forEach {
      it.delete()
      cache.clear()
    }
  }

  override fun delete() {
    clear()
  }

  override fun close() {
    delete()
  }
}