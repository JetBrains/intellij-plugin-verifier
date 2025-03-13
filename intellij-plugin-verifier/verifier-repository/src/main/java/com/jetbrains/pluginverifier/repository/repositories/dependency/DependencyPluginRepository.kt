/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.dependency

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * Repository wrapping another repository while mapping plugins into [dependencies][DependencyPluginInfo].
 */
class DependencyPluginRepository(private val delegateRepository: PluginRepository) : PluginRepository {
  override val presentableName: String = "${delegateRepository.presentableName} (used for plugin dependencies)"

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo> =
    delegateRepository.getLastCompatiblePlugins(ideVersion).asDependencies()

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): PluginInfo? =
    delegateRepository.getLastCompatibleVersionOfPlugin(ideVersion, pluginId).asDependency()

  override fun getAllVersionsOfPlugin(pluginId: String): List<PluginInfo> =
    delegateRepository.getAllVersionsOfPlugin(pluginId).asDependencies()

  override fun getPluginsDeclaringModule(moduleId: String, ideVersion: IdeVersion?): List<PluginInfo> =
    delegateRepository.getPluginsDeclaringModule(moduleId, ideVersion).asDependencies()

  private fun List<PluginInfo>.asDependencies(): List<DependencyPluginInfo> = map { DependencyPluginInfo(it) }

  private fun PluginInfo?.asDependency(): PluginInfo? = this?.let { DependencyPluginInfo(it) }

  override fun toString(): String = delegateRepository.toString()
}


