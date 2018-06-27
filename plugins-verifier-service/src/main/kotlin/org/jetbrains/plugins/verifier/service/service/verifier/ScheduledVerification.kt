package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo

/**
 * Descriptor of a plugin and a target against which the plugin is to be verified.
 */
data class ScheduledVerification(
    val updateInfo: UpdateInfo,
    val ideVersion: IdeVersion
) {
  override fun toString() = "$updateInfo against $ideVersion"
}