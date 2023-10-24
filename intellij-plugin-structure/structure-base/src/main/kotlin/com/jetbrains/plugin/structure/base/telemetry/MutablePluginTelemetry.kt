/*
 * Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.plugin.structure.base.telemetry

import com.jetbrains.plugin.structure.base.utils.Bytes
import java.time.Duration

const val PLUGIN_SIZE = "plugin.size"
const val PARSING_DURATION = "plugin.parsing.duration"
const val UNKNOWN_SIZE: Bytes = -1

class MutablePluginTelemetry {
  private val data = mutableMapOf<String, Any>()

  var pluginSize: Bytes
    get() = data[PLUGIN_SIZE] as Bytes
    set(value) {
      data[PLUGIN_SIZE] = value
    }

  var parsingDuration: Duration
    get() = data[PARSING_DURATION] as Duration
    set(value) {
      data[PARSING_DURATION] = value
    }

  operator fun get(key: String): Any? {
    return data[key]
  }

  operator fun set(key: String, value: Any) {
    data[key] = value
  }

  val rawData: Map<String, Any>
    get() = data.toMap()

  fun toImmutable(): PluginTelemetry {
    return PluginTelemetry(this)
  }
}
