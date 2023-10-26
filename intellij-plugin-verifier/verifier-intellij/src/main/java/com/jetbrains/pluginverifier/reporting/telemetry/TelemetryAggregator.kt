package com.jetbrains.pluginverifier.reporting.telemetry

import com.jetbrains.plugin.structure.base.telemetry.MutablePluginTelemetry
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.pluginverifier.repository.PluginInfo

class TelemetryAggregator {

  private val telemetries: MutableMap<PluginCoordinate, PluginTelemetry> = mutableMapOf()

  fun reportTelemetry(pluginInfo: PluginInfo, telemetry: PluginTelemetry) {
    telemetries.merge(pluginInfo.coordinate, telemetry) { existing, new ->
      MutablePluginTelemetry()
        .apply {
          existing.toMap().forEach { (t, u) ->
            this[t] = u
          }
          new.toMap().forEach { (t, u) ->
            this[t] = u
          }
        }
        .toImmutable()
    }
  }

  operator fun get(id: String, version: String): PluginTelemetry? {
    return telemetries[PluginCoordinate(id, version)]
  }

  operator fun get(plugin: PluginInfo): PluginTelemetry? {
    return telemetries[plugin.coordinate]
  }

  private data class PluginCoordinate(val id: String, val version: String)

  private val PluginInfo.coordinate: PluginCoordinate
    get() = PluginCoordinate(pluginId, version)
}
