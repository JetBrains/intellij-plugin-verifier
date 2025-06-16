/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.ClassSearchContext
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.caches.PluginResourceCache
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.nio.file.Path

class PluginDetailsProviderImpl(extractDirectory: Path, private val pluginResourceCache: PluginResourceCache) : AbstractPluginDetailsProvider(extractDirectory) {
  override fun readPluginClasses(pluginInfo: PluginInfo, idePlugin: IdePlugin) =
    IdePluginClassesFinder
      .findPluginClasses(idePlugin,
        additionalKeys = listOf(CompileServerExtensionKey),
        searchContext = ClassSearchContext(pluginResourceCache, extractDirectory))
}
