/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.ClassSearchContext
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.pluginverifier.repository.PluginInfo

class DependencyDetailsProvider(archiveManager: PluginArchiveManager) :
  AbstractPluginDetailsProvider(archiveManager) {
  override fun readPluginClasses(pluginInfo: PluginInfo, idePlugin: IdePlugin): IdePluginClassesLocations {
    return IdePluginClassesFinder.findPluginClasses(
      idePlugin,
      additionalKeys = listOf(CompileServerExtensionKey),
      ClassSearchContext(archiveManager)
    )
  }
}