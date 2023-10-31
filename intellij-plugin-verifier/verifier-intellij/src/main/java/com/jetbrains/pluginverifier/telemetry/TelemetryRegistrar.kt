package com.jetbrains.pluginverifier.telemetry

import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Register telemetry information discovered in the plugin verification process.
 * @see [com.jetbrains.pluginverifier.verifiers.PluginVerificationContext]
 */
interface TelemetryRegistrar {
  fun reportTelemetry(pluginInfo: PluginInfo, telemetry: PluginTelemetry)
}