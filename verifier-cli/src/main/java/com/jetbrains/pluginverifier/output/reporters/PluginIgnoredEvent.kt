package com.jetbrains.pluginverifier.output.reporters

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Data class used to group information on
 * why the [pluginInfo] was excluded from the
 * verification against [verificationTarget]
 * because of the [reason].
 */
data class PluginIgnoredEvent(
    val pluginInfo: PluginInfo,
    val verificationTarget: VerificationTarget,
    val reason: String
) {
  override fun toString() = "Verification of $pluginInfo against $verificationTarget is ignored: $reason"
}