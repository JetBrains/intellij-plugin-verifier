package com.jetbrains.pluginverifier.persistence

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * The class allows to serialize [Jsonable]s.
 *
 * The serialized form looks as follows:
 * `{&quot;class&quot;: &quot;class_name&quot;, &quot;content&quot;: {&quot;param_1&quot;: &quot;value_1&quot;, ... &quot;param_n&quot;: &quot;value_n&quot;}}`
 * @author Sergey Patrikeev
 */
class JsonableTypeAdapter<T> : TypeAdapter<Jsonable<T>>() {
  private val myFactories = ConcurrentHashMap<String, Jsonable<*>>()

  @Throws(IOException::class)
  override fun write(out: JsonWriter, value: Jsonable<T>) {
    out.beginArray();
    try {
      out.value(value.javaClass.name);
      out.beginArray();
      try {
        for (pair in value.serialize()) {
          out.value(pair.second)
        }
      } finally {
        out.endArray();
      }
    } finally {
      out.endArray();
    }
  }

  @Throws(IOException::class)
  override fun read(`in`: JsonReader): Jsonable<T> {
    `in`.beginArray()
    try {
      val clsName = `in`.nextString()
      val factory = getFactory(clsName)

      `in`.beginArray()
      try {
        val params: MutableList<String?> = arrayListOf() //it is not optimal for expected 1-3 parameters, but it produces cleaner code

        while (`in`.hasNext()) {
          if (`in`.peek() == JsonToken.NULL) {
            params.add(null)
            `in`.nextNull()
          } else {
            params.add(`in`.nextString())
          }
        }

        return factory.deserialize(*params.toTypedArray()) as Jsonable<T>
      } finally {
        `in`.endArray()
      }
    } finally {
      `in`.endArray()
    }
  }

  private fun getFactory(clsName: String): Jsonable<*> {
    var result: Jsonable<*>? = myFactories[clsName]
    if (result == null) {
      //we are not worried about multiple initiations of factory instances.
      try {
        val aClass = Class.forName(clsName)
        if (!Jsonable::class.java.isAssignableFrom(aClass)) {
          throw IllegalArgumentException("Supplied class is not an instance of com.jetbrains.pluginverifier.persistence.Jsonable class")
        }
        result = aClass.newInstance() as Jsonable<*>
        myFactories.put(clsName, result)
      } catch (e: Exception) {
        throw IllegalArgumentException("Unable to initialize a Jsonable class factory $clsName. Check that the Jsonable class contains a public default constructor", e)
      }

    }
    return result
  }


}
