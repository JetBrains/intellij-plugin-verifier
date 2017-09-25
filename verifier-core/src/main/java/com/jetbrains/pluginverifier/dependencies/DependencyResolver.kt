package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.UpdateInfo
import java.io.Closeable

interface DependencyResolver {

  fun resolve(dependency: PluginDependency): Result

  sealed class Result : Closeable {
    data class FoundReady(val plugin: IdePlugin, val pluginClassesLocations: IdePluginClassesLocations) : Result() {
      //must not be closed because it belongs to client.
      override fun close() = Unit
    }

    data class CreatedResolver(val plugin: IdePlugin, val pluginClassesLocations: IdePluginClassesLocations) : Result() {
      override fun close() = pluginClassesLocations.closeLogged()
    }

    data class Downloaded(val plugin: IdePlugin,
                          val pluginClassesLocations: IdePluginClassesLocations,
                          val updateInfo: UpdateInfo,
                          private val pluginFileLock: FileLock) : Result() {
      override fun close() {
        pluginFileLock.closeLogged()
        pluginClassesLocations.closeLogged()
      }
    }

    data class ProblematicDependency(val pluginErrorsAndWarnings: List<PluginProblem>) : Result() {
      override fun close() = Unit
    }

    data class NotFound(val reason: String) : Result() {
      override fun close() = Unit
    }

    data class FailedToDownload(val reason: String) : Result() {
      override fun close() = Unit
    }

    object Skip : Result() {
      override fun close() = Unit
    }
  }
}