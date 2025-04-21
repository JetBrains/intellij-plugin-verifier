/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.bundled

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.VERSION_COMPARATOR

/**
 * [PluginRepository] consisting of plugins bundled to the [IDE] [ide].
 */
class BundledPluginsRepository(
  val ide: Ide
) : PluginRepository {
  private fun getAllPlugins() = ide.bundledPlugins.map {
    BundledPluginInfo(ide.version, it)
  }

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion) =
    getAllPlugins()
      .filter { it.isCompatibleWith(ideVersion) }
      .groupBy { it.pluginId }
      .mapValues { it.value.maxWithOrNull(VERSION_COMPARATOR)!! }
      .values.toList()

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String) =
    getAllVersionsOfPlugin(pluginId).filter { it.isCompatibleWith(ideVersion) }.maxWithOrNull(VERSION_COMPARATOR)

  override fun getAllVersionsOfPlugin(pluginId: String) =
    getAllPlugins().filter { it.pluginId == pluginId }

  override fun getPluginsDeclaringModule(moduleId: String, ideVersion: IdeVersion?): List<PluginInfo> {
    val pluginInfo = findPluginByModule(moduleId)
    return listOfNotNull(pluginInfo?.takeIf { ideVersion == null || pluginInfo.isCompatibleWith(ideVersion) })
  }

  fun findPluginByModule(moduleId: String): BundledPluginInfo? = getAllPlugins().find { moduleId in it.idePlugin.definedModules }

  override val presentableName
    get() = "Bundled plugins of ${ide.version}"

  override fun toString() = presentableName
}

