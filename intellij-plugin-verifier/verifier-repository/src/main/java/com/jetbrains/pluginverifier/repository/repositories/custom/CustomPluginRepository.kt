/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.custom

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.cache.memoize
import com.jetbrains.pluginverifier.repository.repositories.VERSION_COMPARATOR
import java.net.URL

/**
 * Base class for all repositories configured for special plugins not available in JetBrains Marketplace.
 */
abstract class CustomPluginRepository : PluginRepository {

  private val allPluginsCache = memoize(expirationInMinutes = 1) { requestAllPlugins() }

  protected abstract fun requestAllPlugins(): List<CustomPluginInfo>

  abstract val repositoryUrl: URL

  fun getAllPlugins(): List<CustomPluginInfo> = allPluginsCache.get()

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion) =
    getAllPlugins()

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String) =
    getAllPlugins().maxWithOrNull(VERSION_COMPARATOR)

  override fun getAllVersionsOfPlugin(pluginId: String) =
    getAllPlugins().filter { it.pluginId == pluginId }

  override fun getPluginsDeclaringModule(moduleId: String, ideVersion: IdeVersion?): List<PluginInfo> = emptyList()

}
