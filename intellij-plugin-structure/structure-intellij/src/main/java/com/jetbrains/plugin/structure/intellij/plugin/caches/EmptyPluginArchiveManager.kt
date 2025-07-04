/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.caches

import com.jetbrains.plugin.structure.intellij.resources.PluginArchiveResource
import java.nio.file.Path

object EmptyPluginArchiveManager : PluginArchiveManager {
  override fun getPluginResource(pluginArtifactPath: Path) = PluginArchiveManager.Result.NotFound
  override fun findFirst(predicate: (PluginArchiveResource) -> Boolean) = PluginArchiveManager.Result.NotFound
  override fun plusAssign(pluginResource: PluginArchiveResource) = Unit
  override fun close() = Unit
  override fun delete() = Unit
}