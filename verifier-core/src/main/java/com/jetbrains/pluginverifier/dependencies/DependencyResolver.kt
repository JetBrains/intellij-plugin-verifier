package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.UpdateInfo
import java.io.Closeable

interface DependencyResolver {

  fun resolve(dependency: PluginDependency): Result

  sealed class Result : Closeable {
    data class FoundReady(val plugin: IdePlugin, val resolver: Resolver) : Result() {
      //resolver must not be closed because it belongs to client.
      override fun close() = Unit
    }

    data class CreatedResolver(val plugin: IdePlugin, val resolver: Resolver) : Result() {
      override fun close() = resolver.close()
    }

    data class Downloaded(val plugin: IdePlugin,
                          val resolver: Resolver,
                          val updateInfo: UpdateInfo,
                          private val pluginFileLock: FileLock) : Result() {
      override fun close() {
        pluginFileLock.release()
        resolver.close()
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