/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.base.plugin.IconTheme
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager.Companion.META_INF
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class PluginIconLoader : PluginDirectoryResourceLoader<PluginIcon> {
  @Throws(IOException::class)
  override fun load(pluginDirectory: Path): List<PluginIcon> {
    return IconTheme.values().mapNotNull { theme ->
      val iconFile = pluginDirectory.resolve(META_INF).resolve(getIconFileName(theme))
      if (iconFile.exists()) {
        PluginIcon(theme, Files.readAllBytes(iconFile), iconFile.simpleName)
      } else {
        null
      }
    }
  }

  private fun getIconFileName(iconTheme: IconTheme) = "pluginIcon${iconTheme.suffix}.svg"
}