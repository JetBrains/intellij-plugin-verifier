/*
 * Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.jetbrains.plugin.structure.base.telemetry

import com.jetbrains.plugin.structure.base.utils.Bytes
import java.time.Duration

const val PLUGIN_ID = "plugin.id"
const val PLUGIN_VERSION = "plugin.version"
const val PLUGIN_SIZE = "plugin.size"
const val PARSING_DURATION = "plugin.parsing.duration"
const val PLUGIN_VERIFICATION_TIME = "plugin.verification.duration"
const val PLUGIN_VERIFIED_CLASSES_COUNT = "plugin.verification.verified.classes.count"
const val UNKNOWN_SIZE: Bytes = -1

class MutablePluginTelemetry : PluginTelemetry() {

  override var pluginSize: Bytes
    get() = data[PLUGIN_SIZE] as Bytes
    set(value) {
      data[PLUGIN_SIZE] = value
    }

  override var parsingDuration: Duration
    get() = data[PARSING_DURATION] as Duration
    set(value) {
      data[PARSING_DURATION] = value
    }

  operator fun set(key: String, value: Any) {
    data[key] = value
  }

  fun merge(telemetry: PluginTelemetry) {
    data.putAll(telemetry.toMap())
  }

  fun toImmutable(): PluginTelemetry {
    return PluginTelemetry(data)
  }
}
