/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import java.io.IOException
import java.nio.file.Path

interface PluginDirectoryResourceLoader<T> {
  @Throws(IOException::class)
  fun load(pluginDirectory: Path): List<T>
}