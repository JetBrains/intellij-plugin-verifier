/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.empty

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo

object EmptyPluginRepository : PluginRepository {
  override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo> = emptyList()

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? = null

  override fun getAllVersionsOfPlugin(pluginId: String): List<PluginInfo> = emptyList()

  override fun getPluginsDeclaringModule(moduleId: String, ideVersion: IdeVersion?): List<PluginInfo> = emptyList()

  override val presentableName
    get() = "Empty repository"

  override fun toString() = presentableName
}