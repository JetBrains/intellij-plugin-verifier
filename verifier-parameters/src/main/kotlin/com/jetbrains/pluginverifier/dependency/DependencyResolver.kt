package com.jetbrains.pluginverifier.dependency

import com.intellij.structure.plugin.Plugin
import com.intellij.structure.plugin.PluginDependency
import com.intellij.structure.problems.PluginProblem
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.UpdateInfo
import java.io.Closeable

interface DependencyResolver {

  fun resolve(dependency: PluginDependency, isModule: Boolean): Result

  sealed class Result : Closeable {
    data class FoundReady(val plugin: Plugin, val resolver: Resolver) : Result() {
      //resolver must not be closed because it belongs to client.
      override fun close() = Unit
    }

    data class CreatedResolver(val plugin: Plugin, val resolver: Resolver) : Result() {
      override fun close() = resolver.close()
    }

    data class Downloaded(val plugin: Plugin,
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

    object Skip : Result() {
      override fun close() = Unit
    }
  }
}