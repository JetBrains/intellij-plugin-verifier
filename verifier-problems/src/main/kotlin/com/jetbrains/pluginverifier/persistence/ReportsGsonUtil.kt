package com.jetbrains.pluginverifier.persistence

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.typeToken
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.api.VResults
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.location.CodeLocation
import com.jetbrains.pluginverifier.location.PluginLocation
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.*
import com.jetbrains.pluginverifier.problems.fields.ChangeFinalFieldProblem
import com.jetbrains.pluginverifier.problems.statics.*
import com.jetbrains.pluginverifier.results.ResultsElement
import com.jetbrains.pluginverifier.utils.RuntimeTypeAdapterFactory
import java.io.File
import java.io.IOException
import java.lang.reflect.ParameterizedType

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
      .registerTypeAdapter(UpdateInfo::class.java, UpdateInfoTypeAdapter())
      .registerTypeAdapter(File::class.java, FileTypeAdapter())
      .registerTypeHierarchyAdapter(IdeVersion::class.java, IdeVersionTypeAdapter())
      .registerTypeAdapterFactory(problemsTAF)
      .registerTypeAdapterFactory(locationTAF)
      .registerTypeAdapterFactory(pluginDescriptorTAF)
      .registerTypeAdapterFactory(ideDescriptorTAF)
      .registerTypeAdapterFactory(MultimapTypeAdapterFactory())
      .create()
}

//add inheritors
private val problemsTAF = RuntimeTypeAdapterFactory.of(Problem::class.java)
    .registerSubtype(ChangeFinalFieldProblem::class.java)
    .registerSubtype(InstanceAccessOfStaticFieldProblem::class.java)
    .registerSubtype(InvokeInterfaceOnStaticMethodProblem::class.java)
    .registerSubtype(InvokeSpecialOnStaticMethodProblem::class.java)
    .registerSubtype(InvokeStaticOnInstanceMethodProblem::class.java)
    .registerSubtype(InvokeVirtualOnStaticMethodProblem::class.java)
    .registerSubtype(StaticAccessOfInstanceFieldProblem::class.java)
    .registerSubtype(AbstractClassInstantiationProblem::class.java)
    .registerSubtype(BrokenPluginProblem::class.java)
    .registerSubtype(ClassNotFoundProblem::class.java)
    .registerSubtype(CyclicDependenciesProblem::class.java)
    .registerSubtype(FailedToReadClassProblem::class.java)
    .registerSubtype(FieldNotFoundProblem::class.java)
    .registerSubtype(IllegalFieldAccessProblem::class.java)
    .registerSubtype(IllegalMethodAccessProblem::class.java)
    .registerSubtype(IncompatibleClassChangeProblem::class.java)
    .registerSubtype(InterfaceInstantiationProblem::class.java)
    .registerSubtype(InvokeInterfaceOnPrivateMethodProblem::class.java)
    .registerSubtype(MethodNotFoundProblem::class.java)
    .registerSubtype(MethodNotImplementedProblem::class.java)
    .registerSubtype(MissingDependencyProblem::class.java)
    .registerSubtype(NoCompatibleUpdatesProblem::class.java)
    .registerSubtype(OverridingFinalMethodProblem::class.java)


private val locationTAF = RuntimeTypeAdapterFactory.of(ProblemLocation::class.java)
    .registerSubtype(PluginLocation::class.java)
    .registerSubtype(CodeLocation::class.java)

private val pluginDescriptorTAF = RuntimeTypeAdapterFactory.of(PluginDescriptor::class.java)
    .registerSubtype(PluginDescriptor.ByBuildId::class.java)
    .registerSubtype(PluginDescriptor.ByFile::class.java)
    .registerSubtype(PluginDescriptor.ByInstance::class.java)
    .registerSubtype(PluginDescriptor.ByXmlId::class.java)
    .registerSubtype(PluginDescriptor.ByUpdateInfo::class.java)

private val ideDescriptorTAF = RuntimeTypeAdapterFactory.of(IdeDescriptor::class.java)
    .registerSubtype(IdeDescriptor.ByVersion::class.java)
    .registerSubtype(IdeDescriptor.ByFile::class.java)
    .registerSubtype(IdeDescriptor.ByInstance::class.java)

class FileTypeAdapter : TypeAdapter<File>() {

  @Throws(IOException::class)
  override fun write(out: JsonWriter, value: File) {
    out.value(value.absolutePath)
  }

  @Throws(IOException::class)
  override fun read(`in`: JsonReader): File {
    return File(`in`.nextString())
  }

}


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


/**
 * The class allows to serialize all the `Multimap&lt;K, V&gt;` generified classes.
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

fun resultsElementToVResult(element: ResultsElement): VResults {
  val list: MutableList<VResult> = arrayListOf()

  val ideDescriptor = IdeDescriptor.ByVersion(IdeVersion.createIdeVersion(element.ide))
  val overview = ""
  element.asMap().forEach {
    val pluginDescriptor = PluginDescriptor.ByUpdateInfo(it.key)

    val location = ProblemLocation.fromPlugin(it.key.pluginId ?: it.key.pluginName ?: "unknown")

    val multimap = multimapFromMap(it.value.groupBy({ it }, { location }))

    list.add(if (multimap.isEmpty) VResult.Nice(pluginDescriptor, ideDescriptor, overview) else VResult.Problems(pluginDescriptor, ideDescriptor, overview, multimap))
  }

  return VResults(list)
}

/**
 * Creates a Guava multimap using the input map.
 */
fun <K, V> multimapFromMap(input: Map<K, Iterable<V>>): Multimap<K, V> {
  val result = ArrayListMultimap.create<K, V>()
  for (entry in input.entries) {
    result.putAll(entry.key, entry.value)
  }
  return result
}