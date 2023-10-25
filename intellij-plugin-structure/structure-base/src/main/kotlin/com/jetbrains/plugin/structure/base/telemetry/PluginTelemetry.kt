package com.jetbrains.plugin.structure.base.telemetry

import com.jetbrains.plugin.structure.base.utils.Bytes
import java.time.Duration

class PluginTelemetry internal constructor(telemetry: MutablePluginTelemetry) {
  private val data: Map<String, Any>

  init {
    data = telemetry.rawData
  }

  constructor() : this(MutablePluginTelemetry())

  constructor(vararg pairs: Pair<String, Any>) : this(MutablePluginTelemetry(*pairs))

  val pluginSize: Bytes
    get() = data[PLUGIN_SIZE] as Bytes

  val parsingDuration: Duration
    get() = data[PARSING_DURATION] as Duration

  operator fun get(key: String): Any? {
    return data[key]
  }

  override fun toString(): String {
    return data.toString()
  }
}