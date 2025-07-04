/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.classes.plugin

import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.intellij.plugin.caches.EmptyPluginArchiveManager
import com.jetbrains.plugin.structure.intellij.plugin.caches.PluginArchiveManager
import java.nio.file.Path

data class ClassSearchContext(
  val pluginCache: PluginArchiveManager = EmptyPluginArchiveManager,
  val extractDirectory: Path = Settings.EXTRACT_DIRECTORY.getAsPath().createDir()
) {
  companion object {
    @JvmStatic
    val DEFAULT = ClassSearchContext()
  }
}