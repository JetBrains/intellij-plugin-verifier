package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.UpdateInfo

/**
 * Descriptor of the plugin and IDE against which the plugin is to be verified.
 */
data class PluginAndIdeVersion(val updateInfo: UpdateInfo, val ideVersion: IdeVersion) {
  override fun toString() = "$updateInfo against $ideVersion"
}