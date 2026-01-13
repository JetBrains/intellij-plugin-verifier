/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.local

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.CompatibilityPredicate
import com.jetbrains.pluginverifier.repository.repositories.VERSION_COMPARATOR

/**
 * [PluginRepository] consisting of [locally] [LocalPluginInfo] stored plugins.
 */
class LocalPluginRepository(
  private val plugins: MutableList<LocalPluginInfo> = arrayListOf(),
  private val compatibilityPredicate: CompatibilityPredicate = CompatibilityPredicate.DEFAULT
) : PluginRepository {

  fun addLocalPlugin(idePlugin: IdePlugin): LocalPluginInfo {
    val localPluginInfo = LocalPluginInfo(idePlugin)
    plugins.add(localPluginInfo)
    return localPluginInfo
  }

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion) =
    plugins.filter(compatibleWith(ideVersion))
      .groupBy { it.pluginId }
      .mapValues { it.value.maxWithOrNull(VERSION_COMPARATOR)!! }
      .values.toList()

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String) =
    getAllVersionsOfPlugin(pluginId)
      .filter(compatibleWith(ideVersion))
      .maxWithOrNull(VERSION_COMPARATOR)

  override fun getAllVersionsOfPlugin(pluginId: String) =
    plugins.filter { it.pluginId == pluginId }

  override fun getPluginsDeclaringModule(moduleId: String, ideVersion: IdeVersion?) =
    plugins.filter {
      it.idePlugin.hasDefinedModuleWithId(moduleId) && (ideVersion == null || compatibilityPredicate.isCompatible(
        it,
        ideVersion
      ))
    }

  override val presentableName
    get() = "Local Plugin Repository"

  override fun toString() = presentableName

  private fun compatibleWith(ideVersion: IdeVersion): (LocalPluginInfo) -> Boolean =
    { it: LocalPluginInfo -> compatibilityPredicate.isCompatible(it, ideVersion) }
}