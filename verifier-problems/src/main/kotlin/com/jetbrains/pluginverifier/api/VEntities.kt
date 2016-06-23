package com.jetbrains.pluginverifier.api

import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.domain.Plugin
import com.jetbrains.pluginverifier.persistence.Jsonable
import com.jetbrains.pluginverifier.persistence.fromGson
import com.jetbrains.pluginverifier.persistence.notNullize
import java.io.File

/**
 * @author Sergey Patrikeev
 */

/**
 * Descriptor of the plugin to be checked
 */
sealed class PluginDescriptor() : Jsonable<PluginDescriptor> {
  class ByXmlId(val pluginId: String, val version: String? = null) : PluginDescriptor() {
    override fun serialize(): List<Pair<String, String>> = listOf("pluginId" to pluginId, "version" to version.notNullize(""))

    override fun deserialize(vararg params: String?): PluginDescriptor = ByXmlId(params[0]!!.fromGson(), params[1]!!.fromGson())

    override fun toString(): String {
      return "PluginDescriptor.ByXmlId(pluginId='$pluginId', version=$version)"
    }
  }

  class ByBuildId(val buildId: Int) : PluginDescriptor() {
    override fun serialize(): List<Pair<String, String>> = listOf("buildId" to buildId.toString())

    override fun deserialize(vararg params: String?): PluginDescriptor = ByBuildId(params[0]!!.fromGson())

    override fun toString(): String {
      return "PluginDescriptor.ByBuildId(buildId=$buildId)"
    }
  }

  class ByFile(val file: File) : PluginDescriptor() {
    override fun serialize(): List<Pair<String, String>> = listOf("file" to file.absolutePath)

    override fun deserialize(vararg params: String?): PluginDescriptor = ByFile(params[0]!!.fromGson<String>())

    constructor(path: String) : this(File(path))

    override fun toString(): String {
      return "PluginDescriptor.ByFile(file=$file)"
    }

  }

  class ByInstance(val plugin: Plugin) : PluginDescriptor() {
    override fun serialize(): List<Pair<String, String>> = listOf("pluginId" to plugin.pluginId, "version" to plugin.pluginVersion)

    override fun deserialize(vararg params: String?): PluginDescriptor = ByXmlId(params[0]!!.fromGson(), params[1]!!.fromGson())

    override fun toString(): String {
      return "PluginDescriptor.ByInstance(plugin=$plugin)"
    }
  }
}

sealed class IdeDescriptor() : Jsonable<IdeDescriptor> {
  class ByFile(val file: File) : IdeDescriptor() {
    override fun serialize(): List<Pair<String, String?>> = listOf("file" to file.absolutePath)

    override fun deserialize(vararg params: String?): IdeDescriptor = IdeDescriptor.ByFile(params[0]!!)

    constructor(path: String) : this(File(path))

    override fun toString(): String {
      return "IdeDescriptor.ByFile(file=$file)"
    }

  }

  class ByVersion(val version: IdeVersion) : IdeDescriptor() {
    override fun serialize(): List<Pair<String, String?>> = listOf("version" to version.asString())

    override fun deserialize(vararg params: String?): IdeDescriptor = IdeDescriptor.ByVersion(IdeVersion.createIdeVersion(params[0]!!))

    override fun toString(): String {
      return "IdeDescriptor.ByVersion(version=$version)"
    }


  }

  class ByInstance(val ide: Ide) : IdeDescriptor() {
    override fun serialize(): List<Pair<String, String?>> = listOf("ideVersion" to ide.version.asString())

    override fun deserialize(vararg params: String?): IdeDescriptor = IdeDescriptor.ByVersion(IdeVersion.createIdeVersion(params[0]!!))

    override fun toString(): String {
      return "IdeDescriptor.ByInstance(ide=$ide; file=${ide.idePath})"
    }
  }
}