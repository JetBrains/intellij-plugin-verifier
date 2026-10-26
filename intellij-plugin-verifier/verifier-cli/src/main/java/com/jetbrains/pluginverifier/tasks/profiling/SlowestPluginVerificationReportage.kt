package com.jetbrains.pluginverifier.tasks.profiling

import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFICATION_TIME
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Delegates to [delegate] and remembers the plugin with the longest [PLUGIN_VERIFICATION_TIME],
 * as well as the longest verification time of each plugin in [trackedPluginIds].
 *
 * The verification time is the wall-clock time of a single plugin verification,
 * including loading the plugin and resolving its dependencies.
 */
class SlowestPluginVerificationReportage(
  private val delegate: PluginVerificationReportage,
  private val trackedPluginIds: Set<String> = emptySet()
) : PluginVerificationReportage by delegate {

  private val slowest = AtomicReference<PluginVerificationDuration?>()

  private val tracked = ConcurrentHashMap<String, PluginVerificationDuration>()

  val slowestVerification: PluginVerificationDuration?
    get() = slowest.get()

  /**
   * The longest verification time of each plugin in [trackedPluginIds] that was verified, keyed by plugin ID.
   */
  val trackedVerifications: Map<String, PluginVerificationDuration>
    get() = tracked.toMap()

  override fun reportTelemetry(pluginInfo: PluginInfo, telemetry: PluginTelemetry) {
    (telemetry[PLUGIN_VERIFICATION_TIME] as? Duration)?.let { duration ->
      val candidate = PluginVerificationDuration(pluginInfo, duration)
      slowest.updateAndGet { current -> longer(current, candidate) }
      if (pluginInfo.pluginId in trackedPluginIds) {
        tracked.compute(pluginInfo.pluginId) { _, current -> longer(current, candidate) }
      }
    }
    delegate.reportTelemetry(pluginInfo, telemetry)
  }

  private fun longer(current: PluginVerificationDuration?, candidate: PluginVerificationDuration) =
    if (current == null || candidate.duration > current.duration) candidate else current
}

data class PluginVerificationDuration(val plugin: PluginInfo, val duration: Duration)
