package com.jetbrains.pluginverifier.repository.repositories.local

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.io.ObjectInputStream

/**
 * Identifier of a local plugin.
 */
class LocalPluginInfo(
  val idePlugin: IdePlugin
) : PluginInfo(
  idePlugin.pluginId!!,
  idePlugin.pluginName ?: idePlugin.pluginId!!,
  idePlugin.pluginVersion!!,
  idePlugin.sinceBuild,
  idePlugin.untilBuild,
  idePlugin.vendor
) {

  val definedModules: Set<String>
    get() = idePlugin.definedModules

  override val presentableName
    get() = idePlugin.toString()

  private fun writeReplace(): Any = throw UnsupportedOperationException("Local plugins cannot be serialized")

  @Suppress("UNUSED_PARAMETER")
  private fun readObject(stream: ObjectInputStream): Unit = throw UnsupportedOperationException("Local plugins cannot be deserialized")

  override fun equals(other: Any?) = other is LocalPluginInfo && idePlugin == other.idePlugin

  override fun hashCode() = idePlugin.hashCode()

}