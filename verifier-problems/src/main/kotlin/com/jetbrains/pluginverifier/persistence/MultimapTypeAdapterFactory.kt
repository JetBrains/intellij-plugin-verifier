package com.jetbrains.pluginverifier.persistence

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.lang.reflect.ParameterizedType

/**
 * The class allows to serialize all the `Multimap&lt;K, V&gt;` generified classes.

 * @author Sergey Patrikeev
 */
class MultimapTypeAdapterFactory : TypeAdapterFactory {
  override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
    val type = typeToken.type
    if (!Multimap::class.java.isAssignableFrom(typeToken.rawType) || type !is ParameterizedType) {
      return null
    }
    val types = type.actualTypeArguments
    val keyType = types[0]
    val valueType = types[1]
    val keyAdapter = gson.getAdapter(TypeToken.get(keyType))
    val valueAdapter = gson.getAdapter(TypeToken.get(valueType))
    return newMultimapAdapter(keyAdapter, valueAdapter) as TypeAdapter<T>
  }

  private fun <K, V> newMultimapAdapter(keyAdapter: TypeAdapter<K>, valueAdapter: TypeAdapter<V>): TypeAdapter<Multimap<K, V>> {
    return object : TypeAdapter<Multimap<K, V>>() {
      @Throws(IOException::class)
      override fun write(out: JsonWriter, value: Multimap<K, V>) {
        out.beginArray()
        for (k in value.keySet()) {
          keyAdapter.write(out, k)
          out.beginArray()
          for (v in value.get(k)) {
            valueAdapter.write(out, v)
          }
          out.endArray()
        }
        out.endArray()
      }

      @Throws(IOException::class)
      override fun read(`in`: JsonReader): Multimap<K, V> {
        val result = LinkedHashMultimap.create<K, V>()
        `in`.beginArray()
        while (`in`.hasNext()) {
          val k = keyAdapter.read(`in`)
          `in`.beginArray()
          while (`in`.hasNext()) {
            val v = valueAdapter.read(`in`)
            result.put(k, v)
          }
          `in`.endArray()
        }

        `in`.endArray()
        return result
      }
    }.nullSafe() //Gson will check nulls automatically
  }
}
