/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.caches

import com.jetbrains.plugin.structure.base.utils.Deletable
import com.jetbrains.plugin.structure.intellij.resources.PluginArchiveResource
import java.io.Closeable
import java.nio.file.Path

interface PluginArchiveManager : Closeable, Deletable {
  fun getPluginResource(pluginArtifactPath: Path): Result

  fun findFirst(predicate: (PluginArchiveResource) -> Boolean): Result

  operator fun plusAssign(pluginResource: PluginArchiveResource)

  sealed class Result {
    data class Found(val pluginResource: PluginArchiveResource) : Result() {
      override fun toString(): String = with(pluginResource) {
        return "$artifactPath cached to $extractedPath (plugin '$id' at '$version')"
      }
    }
    object NotFound : Result() {
      override fun toString(): String = "Not found in cache"
    }
  }
}