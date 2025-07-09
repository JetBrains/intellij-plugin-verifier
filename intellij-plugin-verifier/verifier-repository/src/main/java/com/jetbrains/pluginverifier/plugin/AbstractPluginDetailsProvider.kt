/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.plugin.structure.intellij.plugin.StructurallyValidated
import com.jetbrains.plugin.structure.intellij.plugin.createIdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.UnableToReadPluginFile
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock

/**
 * Baseline implementation of the [PluginDetailsProvider] that
 * uses the [PluginArchiveManager] for managing and extracting `.zip`-ped plugins.
 */
abstract class AbstractPluginDetailsProvider(protected val archiveManager: PluginArchiveManager) : PluginDetailsProvider {
  protected val idePluginManager = createIdePluginManager(archiveManager)

  private val IdePlugin.problems: List<PluginProblem>
    get() = if (this is StructurallyValidated) this.problems else emptyList()

  abstract fun readPluginClasses(pluginInfo: PluginInfo, idePlugin: IdePlugin): IdePluginClassesLocations

  override fun providePluginDetails(pluginInfo: PluginInfo, pluginFileLock: FileLock) =
    pluginFileLock.closeOnException {
      with(createPlugin(pluginInfo, pluginFileLock)) {
        when (this) {
          is PluginCreationSuccess<IdePlugin> -> {
            readPluginClasses(
              pluginInfo,
              plugin,
              plugin.problems,
              pluginFileLock
            )
          }

          is PluginCreationFail<IdePlugin> -> {
            pluginFileLock.closeLogged<FileLock?>()
            PluginDetailsProvider.Result.InvalidPlugin(pluginInfo, errorsAndWarnings)
          }
        }
      }
    }

  override fun providePluginDetails(
    pluginInfo: PluginInfo,
    idePlugin: IdePlugin
  ): PluginDetailsProvider.Result {
    return readPluginClasses(pluginInfo, idePlugin, idePlugin.problems, null)
  }

  private fun readPluginClasses(
    pluginInfo: PluginInfo,
    idePlugin: IdePlugin,
    warnings: List<PluginProblem>,
    pluginFileLock: FileLock?
  ): PluginDetailsProvider.Result {
    return try {
      readPluginClasses(pluginInfo, idePlugin)
        .let { pluginClassesLocations ->
          PluginDetailsProvider.Result.Provided(
            PluginDetails(
              pluginInfo,
              idePlugin,
              warnings,
              pluginClassesLocations,
              pluginFileLock
            )
          )
        }
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      val message = e.message ?: e.javaClass.simpleName
      PluginDetailsProvider.Result.InvalidPlugin(pluginInfo, listOf(UnableToReadPluginFile(message)))
    }
  }

  protected open fun createPlugin(pluginInfo: PluginInfo, pluginFileLock: FileLock) =
    idePluginManager.createPlugin(pluginFileLock.file)
}