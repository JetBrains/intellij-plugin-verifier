package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.base.utils.Deletable
import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginId
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.id
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(ZipPluginResource::class.java)

/**
 * Represents a plugin resource that has been extracted from a ZIP file.
 *
 * This resource is [java.io.Closeable] to maintain resource contracts.
 * However, to delete an underlying extracted directory, the [delete] method needs to be invoked.
 *
 * @param pluginArtifactPath a path to the plugin ZIP file
 * @param extractedPluginPath a path that contains an extracted plugin. An [com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager]
 * @param id an identifier of the plugin. Corresponds to the `id` or `name` in the descriptor as a fallback
 * @param version plugin version, taken from the plugin descriptor
 * can read the plugin from this path. The last path component will contain the plugin name, for example `plugins/SomePlugin`.
 * This last path component is usually a parent of `lib` folder.
 */
data class ZipPluginResource(val pluginArtifactPath: Path, val extractedPluginPath: Path, val id: PluginId, val version: String) : Closeable, Deletable {
  override fun close() = Unit

  override fun delete() {
    extractedPluginPath.parent.deleteLogged()
  }

  companion object {
    fun of(pluginArtifactPath: Path, extractedPluginPath: Path, plugin: IdePlugin): ZipPluginResource {
      return ZipPluginResource(pluginArtifactPath, extractedPluginPath, plugin.id, plugin.version)
    }

    fun ZipPluginResource.matches(pluginArtifactPath: Path, plugin: IdePlugin): Boolean {
      return this.pluginArtifactPath == pluginArtifactPath
        && this.id == plugin.id
        && this.version == plugin.version
    }

    private val IdePlugin.version: String get() = pluginVersion ?: "0"
  }
}