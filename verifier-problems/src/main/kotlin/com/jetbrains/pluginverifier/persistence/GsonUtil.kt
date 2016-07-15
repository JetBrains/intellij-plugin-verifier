package com.jetbrains.pluginverifier.persistence

import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.location.CodeLocation
import com.jetbrains.pluginverifier.location.PluginLocation
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.*
import com.jetbrains.pluginverifier.problems.fields.ChangeFinalFieldProblem
import com.jetbrains.pluginverifier.problems.statics.*
import com.jetbrains.pluginverifier.report.CheckIdeReport
import com.jetbrains.pluginverifier.report.checkIdeReportDeserializer
import com.jetbrains.pluginverifier.report.checkIdeReportSerializer
import com.jetbrains.pluginverifier.utils.RuntimeTypeAdapterFactory
import java.io.IOException
import java.lang.reflect.ParameterizedType

/**
 * @author Sergey Patrikeev
 */

object GsonHolder {
  val GSON = GsonBuilder()
      .registerTypeHierarchyAdapter(IdeVersion::class.java, IdeVersionTypeAdapter().nullSafe())
      .registerTypeHierarchyAdapter(IdeDescriptor::class.java, IdeDescriptorTypeAdapter().nullSafe())
      .registerTypeAdapterFactory(resultTAF)
      .registerTypeAdapterFactory(problemsTAF)
      .registerTypeAdapterFactory(locationTAF)
      .registerTypeAdapterFactory(pluginDescriptorTAF)
      .registerTypeAdapterFactory(MultimapTypeAdapterFactory())

      //delegate to ByXmlId (we can't serialize File and Ide because it makes no sense)
      .registerTypeAdapter<PluginDescriptor.ByFile> {
        serialize {
          it.context.serialize(PluginDescriptor.ByXmlId(it.src.pluginId, it.src.version))
        }
      }
      .registerTypeAdapter<PluginDescriptor.ByInstance> {
        serialize {
          it.context.serialize(PluginDescriptor.ByXmlId(it.src.pluginId, it.src.version))
        }
      }
      .registerTypeAdapter<CheckIdeReport>(checkIdeReportSerializer)
      .registerTypeAdapter<CheckIdeReport>(checkIdeReportDeserializer)
      .create()
}

private val resultTAF = RuntimeTypeAdapterFactory.of(VResult::class.java)
    .registerSubtype(VResult.Nice::class.java)
    .registerSubtype(VResult.Problems::class.java)
    .registerSubtype(VResult.BadPlugin::class.java)

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
    .registerSubtype(InheritFromFinalClassProblem::class.java)


private val locationTAF = RuntimeTypeAdapterFactory.of(ProblemLocation::class.java)
    .registerSubtype(PluginLocation::class.java)
    .registerSubtype(CodeLocation::class.java)

private val pluginDescriptorTAF = RuntimeTypeAdapterFactory.of(PluginDescriptor::class.java)
    .registerSubtype(PluginDescriptor.ByBuildId::class.java)
    .registerSubtype(PluginDescriptor.ByXmlId::class.java)
    .registerSubtype(PluginDescriptor.ByUpdateInfo::class.java)
//    .registerSubtype(PluginDescriptor.ByFile::class.java) //this class is serialized as ByXmlId
//    .registerSubtype(PluginDescriptor.ByInstance::class.java) //this class is serialized as ByXmlId

class IdeDescriptorTypeAdapter : TypeAdapter<IdeDescriptor>() {
  override fun read(`in`: JsonReader): IdeDescriptor {
    return IdeDescriptor.ByVersion(IdeVersion.createIdeVersion(`in`.nextString()))
  }

  override fun write(out: JsonWriter, value: IdeDescriptor) {
    out.value(value.ideVersion.asString())
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
    @Suppress("UNCHECKED_CAST")
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

/**
 * Creates a Guava multimap using the input map.
 */
fun <K, V> Map<K, Iterable<V>>.multimapFromMap(): Multimap<K, V> {
  val result = ArrayListMultimap.create<K, V>()
  for ((key, values) in this) {
    result.putAll(key, values)
  }
  return result
}