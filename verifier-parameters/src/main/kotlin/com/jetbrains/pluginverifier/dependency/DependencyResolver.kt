package com.jetbrains.pluginverifier.dependency

import com.intellij.structure.plugin.Plugin
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.UpdateInfo
import java.io.Closeable

interface DependencyResolver {

  fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): Result

  sealed class Result : Closeable {
    class FoundLocally(val pluginCreateOk: CreatePluginResult.OK) : Result() {
      override fun close() = pluginCreateOk.close()
    }

    class Downloaded(val pluginCreateOk: CreatePluginResult.OK,
                     val updateInfo: UpdateInfo,
                     private val pluginFileLock: FileLock) : Result() {
      override fun close() {
        try {
          pluginCreateOk.close()
        } finally {
          pluginFileLock.close()
        }
      }
    }

    class ProblematicDependency(val badPluginCreation: CreatePluginResult.BadPlugin) : Result() {
      override fun close() = badPluginCreation.close()
    }

    class NotFound(val reason: String) : Result() {
      override fun close() = Unit
    }

    object Skip : Result() {
      override fun close() = Unit
    }
  }
}