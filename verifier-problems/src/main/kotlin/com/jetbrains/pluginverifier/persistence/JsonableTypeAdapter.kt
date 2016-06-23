package com.jetbrains.pluginverifier.persistence

import com.google.common.base.Preconditions
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
    out.beginObject();
    try {
      out.name(CLASS_FIELD);
      out.value(value.javaClass.name);
      out.name(CONTENT_FIELD);
      out.beginObject();
      try {
        for (pair in value.serialize()) {
          out.name(pair.first)
          out.value(pair.second)
        }
      } finally {
        out.endObject();
      }
    } finally {
      out.endObject();
    }
  }

  @Throws(IOException::class)
  override fun read(`in`: JsonReader): Jsonable<T> {
    `in`.beginObject()
    try {
      Preconditions.checkArgument(CLASS_FIELD == `in`.nextName(), "invalid JSON argument")
      val clsName = `in`.nextString()
      val factory = getFactory(clsName)

      Preconditions.checkArgument(CONTENT_FIELD == `in`.nextName(), "invalid JSON argument")

      `in`.beginObject()
      try {
        val params: MutableList<String?> = arrayListOf() //it is not optimal for expected 1-3 parameters, but it produces cleaner code

        while (`in`.hasNext()) {
          `in`.nextName() //skip parameter name
          if (`in`.peek() == JsonToken.NULL) {
            params.add(null)
            `in`.nextNull()
          } else {
            params.add(`in`.nextString())
          }
        }

        return factory.deserialize(*params.toTypedArray()) as Jsonable<T>
      } finally {
        `in`.endObject()
      }
    } finally {
      `in`.endObject()
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

  companion object {

    private val CLASS_FIELD = "class"
    private val CONTENT_FIELD = "content"
  }


}
