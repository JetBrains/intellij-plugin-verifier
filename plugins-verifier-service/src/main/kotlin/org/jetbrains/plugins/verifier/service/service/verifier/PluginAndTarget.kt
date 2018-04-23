package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.repository.UpdateInfo

/**
 * Descriptor of the plugin and a target against which the plugin is to be verified.
 */
data class PluginAndTarget(val updateInfo: UpdateInfo, val verificationTarget: VerificationTarget) {
  override fun toString() = "$updateInfo against $verificationTarget"
}