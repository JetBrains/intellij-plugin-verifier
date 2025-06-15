/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.caches

import com.jetbrains.plugin.structure.base.utils.Deletable
import com.jetbrains.plugin.structure.intellij.resources.ZipPluginResource
import java.io.Closeable
import java.nio.file.Path

interface PluginResourceCache : Closeable, Deletable {
  fun getPluginResource(pluginArtifactPath: Path): Result

  operator fun plusAssign(pluginResource: ZipPluginResource)

  sealed class Result {
    data class Found(val pluginResource: ZipPluginResource) : Result()
    object NotFound : Result()
  }
}