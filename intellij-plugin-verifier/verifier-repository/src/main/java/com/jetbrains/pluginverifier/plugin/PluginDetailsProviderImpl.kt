/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import java.nio.file.Path

/**
 * Main implementation of the [PluginDetailsProvider] that
 * uses the [extractDirectory] for extracting `.zip`-ped plugins.
 */
class PluginDetailsProviderImpl(private val extractDirectory: Path) : PluginDetailsProvider {
  private val idePluginManager = IdePluginManager.createManager(extractDirectory.toFile())

  @Throws(IllegalArgumentException::class)
  override fun providePluginDetails(pluginFile: Path) = createPluginDetails(pluginFile, null, null)

  override fun providePluginDetails(
    pluginInfo: PluginInfo,
    pluginFileLock: FileLock
  ) = pluginFileLock.closeOnException {
    createPluginDetails(pluginFileLock.file, pluginFileLock, pluginInfo)
  }

  override fun providePluginDetails(
    pluginInfo: PluginInfo,
    idePlugin: IdePlugin
  ) = readPluginClasses(pluginInfo, idePlugin, emptyList(), null)

  private fun createPluginDetails(
    pluginFile: Path,
    pluginFileLock: FileLock?,
    pluginInfo: PluginInfo?
  ) = with(idePluginManager.createPlugin(pluginFile.toFile())) {
    when (this) {
      is PluginCreationSuccess -> {
        readPluginClasses(
          pluginInfo ?: LocalPluginInfo(plugin),
          plugin,
          warnings,
          pluginFileLock
        )
      }

      is PluginCreationFail -> {
        if (pluginInfo == null) {
          throw IllegalArgumentException("Invalid plugin from file $pluginFile: ${errorsAndWarnings.joinToString()}")
        }
        pluginFileLock.closeLogged()
        PluginDetailsProvider.Result.InvalidPlugin(pluginInfo, errorsAndWarnings)
      }
    }
  }

  private fun readPluginClasses(
    pluginInfo: PluginInfo,
    idePlugin: IdePlugin,
    warnings: List<PluginProblem>,
    pluginFileLock: FileLock?
  ): PluginDetailsProvider.Result {

    val pluginClassesLocations = try {
      IdePluginClassesFinder.findPluginClasses(idePlugin, additionalKeys = listOf(CompileServerExtensionKey))
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      return PluginDetailsProvider.Result.Failed("Unable to read class files of plugin $pluginInfo", e)
    }

    return PluginDetailsProvider.Result.Provided(
      PluginDetails(
        pluginInfo,
        idePlugin,
        warnings,
        pluginClassesLocations,
        pluginFileLock
      )
    )
  }

}
