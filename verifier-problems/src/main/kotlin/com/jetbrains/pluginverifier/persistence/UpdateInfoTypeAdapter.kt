package com.jetbrains.pluginverifier.persistence

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.jetbrains.pluginverifier.format.UpdateInfo

import java.io.IOException

/**
 * @author Sergey Patrikeev
 */
class UpdateInfoTypeAdapter : TypeAdapter<UpdateInfo>() {
  @Throws(IOException::class)
  override fun write(out: JsonWriter, value: UpdateInfo) {
    out.beginArray()
    out.value(value.pluginId)
    out.value(value.pluginName)
    out.value(value.version)
    out.value(value.updateId)
    //we don't serialize CDate because it's useless.
    out.endArray()
  }

  @Throws(IOException::class)
  override fun read(`in`: JsonReader): UpdateInfo {
    `in`.beginArray()
    val id = getStringOrNull(`in`)
    val name = getStringOrNull(`in`)
    val version = getStringOrNull(`in`)
    var updateId: Int? = null

    if (`in`.peek() == JsonToken.NULL) {
      `in`.nextNull()
    } else {
      updateId = `in`.nextInt()
    }

    `in`.endArray()
    return UpdateInfo(updateId, id, name, version)
  }

  @Throws(IOException::class)
  private fun getStringOrNull(`in`: JsonReader): String? {
    if (`in`.peek() == JsonToken.NULL) {
      `in`.nextNull()
      return null
    } else {
      return `in`.nextString()
    }
  }

}
