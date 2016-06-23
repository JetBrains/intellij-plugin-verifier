package com.jetbrains.pluginverifier.persistence

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.typeToken
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.format.UpdateInfo
import java.io.IOException

/**
 * @author Sergey Patrikeev
 */
inline fun <reified T : Any> Any.toGsonTyped(): String = GsonHolder.GSON.toJson(this, typeToken<T>())

fun Any.toGson(): String = GsonHolder.GSON.toJson(this)

fun Any.toGsonTree() = GsonHolder.GSON.toJsonTree(this)

inline fun <reified T : Any> String.fromGson() = GsonHolder.GSON.fromJson<T>(this)

fun <T> T?.notNullize(default: T) = if (this == null) default else this

object GsonHolder {
  val GSON = GsonBuilder()
      .serializeNulls()
      .registerTypeAdapter(UpdateInfo::class.java, UpdateInfoTypeAdapter())
      .registerTypeHierarchyAdapter(IdeVersion::class.java, IdeVersionTypeAdapter())
      .registerTypeHierarchyAdapter(Jsonable::class.java, JsonableTypeAdapter<Any>()) //Problems serializer/deserializer
      .registerTypeAdapterFactory(MultimapTypeAdapterFactory())
      .create()

}

/**
 * @author Sergey Patrikeev
 */
class IdeVersionTypeAdapter : TypeAdapter<IdeVersion>() {

  @Throws(IOException::class)
  override fun write(out: JsonWriter, value: IdeVersion) {
    out.value(value.asString())
  }

  @Throws(IOException::class)
  override fun read(`in`: JsonReader): IdeVersion {
    return IdeVersion.createIdeVersion(`in`.nextString())
  }

}

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