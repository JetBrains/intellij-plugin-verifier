package com.jetbrains.pluginverifier.api

import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.domain.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.format.UpdateInfo
import java.io.File

/**
 * @author Sergey Patrikeev
 */

/**
 * Descriptor of the plugin to be checked
 */
sealed class PluginDescriptor(@SerializedName("id") val pluginId: String,
                              @SerializedName("version") val version: String) {

  private fun id() = this.javaClass.name + this.toString()

  override fun equals(other: Any?): Boolean {
    if (other is PluginDescriptor) {
      return this.id().equals(other.id())
    }
    return false
  }

  override fun hashCode(): Int {
    return this.id().hashCode()
  }

  class ByUpdateInfo(pluginId: String, version: String, @SerializedName("update") val updateInfo: UpdateInfo) : PluginDescriptor(pluginId, version) {
    override fun toString(): String {
      return "PluginDescriptor.ByUpdateInfo(updateInfo=$updateInfo)"
    }
  }

  class ByXmlId(pluginId: String, version: String) : PluginDescriptor(pluginId, version) {

    override fun toString(): String {
      return "PluginDescriptor.ByXmlId(pluginId='$pluginId', version=$version)"
    }
  }

  class ByBuildId(pluginId: String, version: String, @SerializedName("buildId") val buildId: Int) : PluginDescriptor(pluginId, version) {
    override fun toString(): String {
      return "PluginDescriptor.ByBuildId(buildId=$buildId)"
    }
  }

  class ByFile(pluginId: String, version: String, @Transient val file: File) : PluginDescriptor(pluginId, version) {

    override fun toString(): String {
      return "PluginDescriptor.ByFile(file=$file)"
    }

  }

  //this class is not intended to be serialized yet.
  class ByInstance(@Transient val plugin: Plugin) : PluginDescriptor(plugin.pluginId ?: plugin.pluginName, plugin.pluginVersion) {

    override fun toString(): String {
      return "PluginDescriptor.ByInstance(plugin=$plugin)"
    }
  }
}

sealed class IdeDescriptor(@SerializedName("version") val ideVersion: IdeVersion) {

  private fun id() = this.javaClass.name + this.toString()

  override fun equals(other: Any?): Boolean {
    if (other is IdeDescriptor) {
      return this.id().equals(other.id())
    }
    return false
  }

  override fun hashCode(): Int {
    return this.id().hashCode()
  }

  class ByFile(ideVersion: IdeVersion, @SerializedName("file") val file: File) : IdeDescriptor(ideVersion) {

    override fun toString(): String {
      return "IdeDescriptor.ByFile(file=$file)"
    }

  }

  class ByVersion(ideVersion: IdeVersion) : IdeDescriptor(ideVersion) {
    override fun toString(): String {
      return "IdeDescriptor.ByVersion(version=$ideVersion)"
    }
  }

  class ByInstance(@Transient val ide: Ide, @Transient val ideResolver: Resolver? = null) : IdeDescriptor(ide.version) {

    override fun toString(): String {
      return "IdeDescriptor.ByInstance(ide=$ide; file=${ide.idePath})"
    }
  }
}

sealed class JdkDescriptor() {
  class ByFile(val file: File) : JdkDescriptor() {
    constructor(path: String) : this(File(path))

    override fun toString(): String {
      return "JdkDescriptor.ByFile(file=$file)"
    }

  }

}