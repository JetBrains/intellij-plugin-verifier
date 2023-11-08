package com.jetbrains.plugin.structure.base.telemetry

import com.jetbrains.plugin.structure.base.utils.Bytes
import java.time.Duration

open class PluginTelemetry {
  protected val data: MutableMap<String, Any> = mutableMapOf()

  internal constructor(from: Map<String, Any>) {
    data.putAll(from)
  }

  constructor(vararg pairs: Pair<String, Any>) {
    data.putAll(pairs)
  }

  open val archiveFileSize: Bytes
    get() = data[ARCHIVE_FILE_SIZE] as Bytes

  open val parsingDuration: Duration
    get() = data[PARSING_DURATION] as Duration

  operator fun get(key: String): Any? {
    return data[key]
  }

  override fun toString(): String {
    return data.toString()
  }

  fun toMap(): Map<String, Any> {
    return data.toMap()
  }
}