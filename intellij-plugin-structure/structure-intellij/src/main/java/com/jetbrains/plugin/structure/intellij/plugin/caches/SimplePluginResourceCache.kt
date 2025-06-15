/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.caches

import com.jetbrains.plugin.structure.base.utils.Deletable
import com.jetbrains.plugin.structure.intellij.resources.ZipPluginResource
import java.io.Closeable
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList

class SimplePluginResourceCache : PluginResourceCache, Deletable, Closeable {
  private val cache = CopyOnWriteArrayList<ZipPluginResource>()

  override fun getPluginResource(pluginArtifactPath: Path): PluginResourceCache.Result {
    return cache.find { it.pluginArtifactPath == pluginArtifactPath }
      ?.let { PluginResourceCache.Result.Found(it) }
      ?: PluginResourceCache.Result.NotFound
  }

  override fun plusAssign(pluginResource: ZipPluginResource) {
    cache.addIfAbsent(pluginResource)
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