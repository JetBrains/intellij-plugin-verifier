package com.jetbrains.pluginverifier.tasks.profiling

import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFICATION_TIME
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

/**
 * Delegates to [delegate] and remembers the plugin with the longest [PLUGIN_VERIFICATION_TIME].
 *
 * The verification time is the wall-clock time of a single plugin verification,
 * including loading the plugin and resolving its dependencies.
 */
class SlowestPluginVerificationReportage(
  private val delegate: PluginVerificationReportage
) : PluginVerificationReportage by delegate {

  private val slowest = AtomicReference<PluginVerificationDuration?>()

  val slowestVerification: PluginVerificationDuration?
    get() = slowest.get()

  override fun reportTelemetry(pluginInfo: PluginInfo, telemetry: PluginTelemetry) {
    (telemetry[PLUGIN_VERIFICATION_TIME] as? Duration)?.let { duration ->
      val candidate = PluginVerificationDuration(pluginInfo, duration)
      slowest.updateAndGet { current ->
        if (current == null || candidate.duration > current.duration) candidate else current
      }
    }
    delegate.reportTelemetry(pluginInfo, telemetry)
  }
}

data class PluginVerificationDuration(val plugin: PluginInfo, val duration: Duration)
