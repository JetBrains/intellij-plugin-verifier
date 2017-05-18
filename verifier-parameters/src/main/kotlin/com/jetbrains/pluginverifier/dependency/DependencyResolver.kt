package com.jetbrains.pluginverifier.dependency

import com.intellij.structure.plugin.Plugin
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import java.io.Closeable

interface DependencyResolver {

  fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): Result

  sealed class Result : Closeable {
    class Found(val pluginCreateOk: CreatePluginResult.OK) : Result() {
      override fun close() = pluginCreateOk.close()
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