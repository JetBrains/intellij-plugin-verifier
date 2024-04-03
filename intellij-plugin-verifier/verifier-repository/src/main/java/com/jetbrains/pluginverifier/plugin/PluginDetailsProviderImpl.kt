/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.StructurallyValidated
import com.jetbrains.plugin.structure.intellij.problems.UnableToReadPluginFile
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import java.nio.file.Path

/**
 * Main implementation of the [PluginDetailsProvider] that
 * uses the [extractDirectory] for extracting `.zip`-ped plugins.
 */
class PluginDetailsProviderImpl(private val extractDirectory: Path) : PluginDetailsProvider {
  private val idePluginManager = IdePluginManager.createManager(extractDirectory)

  override fun providePluginDetails(pluginInfo: PluginInfo, pluginFileLock: FileLock) =
    pluginFileLock.closeOnException {
      with(idePluginManager.createPlugin(pluginFileLock.file)) {
        when (this) {
          is PluginCreationSuccess -> {
            readPluginClasses(
              pluginInfo,
              plugin,
              plugin.problems,
              pluginFileLock
            )
          }

          is PluginCreationFail -> {
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

    val pluginClassesLocations = try {
      IdePluginClassesFinder.findPluginClasses(idePlugin, additionalKeys = listOf(CompileServerExtensionKey))
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      val message = e.message ?: e.javaClass.simpleName
      return PluginDetailsProvider.Result.InvalidPlugin(pluginInfo, listOf(UnableToReadPluginFile(message)))
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

  private val IdePlugin.problems: List<PluginProblem>
    get() = if (this is StructurallyValidated) this.problems else emptyList()

}
