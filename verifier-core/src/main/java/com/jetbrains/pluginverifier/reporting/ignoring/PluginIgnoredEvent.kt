package com.jetbrains.pluginverifier.reporting.ignoring

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Data class used to group information on
 * why the [pluginInfo] was excluded from the
 * verification with [ideVersion].
 * The detailed reason is [reason].
 */
data class PluginIgnoredEvent(
    val pluginInfo: PluginInfo,
    val ideVersion: IdeVersion,
    val reason: String
) {
  override fun toString() = "Verification of $pluginInfo against $ideVersion is ignored: $reason"
}