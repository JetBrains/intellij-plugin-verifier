package com.jetbrains.pluginverifier.reporting.telemetry

import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.pluginverifier.repository.PluginInfo

private typealias PluginCoordinate = String

class TelemetryAggregator {

  private val telemetries: MutableMap<PluginCoordinate, List<PluginTelemetry>> = mutableMapOf()

  fun reportTelemetry(pluginInfo: PluginInfo, telemetry: PluginTelemetry) {
    telemetries.merge(pluginInfo.coordinate, listOf(telemetry)) { existing, new -> existing + new }
  }

  operator fun get(id: String, version: String): List<PluginTelemetry> {
    return telemetries[PluginCoordinate(id, version)].orEmpty()
  }

  operator fun get(plugin: PluginInfo): List<PluginTelemetry> {
    return telemetries[plugin.coordinate].orEmpty()
  }

  private data class PluginCoordinate(val id: String, val version: String)

  private val PluginInfo.coordinate: PluginCoordinate
    get() = PluginCoordinate(pluginId, version)
}
