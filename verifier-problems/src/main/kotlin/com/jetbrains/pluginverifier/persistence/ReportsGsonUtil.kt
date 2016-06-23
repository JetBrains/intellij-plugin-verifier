package com.jetbrains.pluginverifier.persistence

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.format.UpdateInfo

/**
 * @author Sergey Patrikeev
 */
fun Any.toGson(): String = GsonHolder.GSON.toJson(this)

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