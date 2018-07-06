package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import java.util.*

/**
 * Descriptor of a plugin and a target against which the plugin is to be verified.
 * [manually] indicates whether this verification has been scheduled by a user
 * meaning that it has more priority for execution than automatically scheduled ones.
 */
data class ScheduledVerification(
    val updateInfo: UpdateInfo,
    val ideVersion: IdeVersion,
    val manually: Boolean = false
) {
  override fun toString() = "$updateInfo against $ideVersion"

  override fun equals(other: Any?) = other is ScheduledVerification &&
      updateInfo == other.updateInfo &&
      ideVersion == other.ideVersion

  override fun hashCode() = Objects.hash(updateInfo, ideVersion)
}