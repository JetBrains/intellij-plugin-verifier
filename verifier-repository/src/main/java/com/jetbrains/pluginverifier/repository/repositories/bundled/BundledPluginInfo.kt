package com.jetbrains.pluginverifier.repository.repositories.bundled

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.io.ObjectInputStream
import java.util.*

/**
 * Identifier of a plugin bundled to IDE.
 */
class BundledPluginInfo(
    val ideVersion: IdeVersion,
    val idePlugin: IdePlugin
) : PluginInfo(
    idePlugin.pluginId!!,
    idePlugin.pluginName ?: idePlugin.pluginId!!,
    idePlugin.pluginVersion ?: ideVersion.asString(),
    idePlugin.sinceBuild,
    idePlugin.untilBuild,
    idePlugin.vendor
) {

  private fun writeReplace(): Any = throw UnsupportedOperationException("Bundled plugins cannot be serialized")

  @Suppress("UNUSED_PARAMETER")
  private fun readObject(stream: ObjectInputStream): Unit = throw UnsupportedOperationException("Bundled plugins cannot be deserialized")

  override fun equals(other: Any?) = other is BundledPluginInfo
      && ideVersion == other.ideVersion
      && idePlugin == other.idePlugin

  override fun hashCode() = Objects.hash(ideVersion, idePlugin)

}