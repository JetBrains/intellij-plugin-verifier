package com.jetbrains.pluginverifier.api

import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.domain.Plugin
import com.jetbrains.pluginverifier.format.UpdateInfo
import java.io.File

/**
 * @author Sergey Patrikeev
 */

/**
 * Descriptor of the plugin to be checked
 */
sealed class PluginDescriptor() {

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

  class ByUpdateInfo(@SerializedName("update") val updateInfo: UpdateInfo) : PluginDescriptor() {
    override fun toString(): String {
      return "PluginDescriptor.ByUpdateInfo(updateInfo=$updateInfo)"
    }
  }

  class ByXmlId(@SerializedName("id") val pluginId: String, @SerializedName("version") val version: String? = null) : PluginDescriptor() {

    override fun toString(): String {
      return "PluginDescriptor.ByXmlId(pluginId='$pluginId', version=$version)"
    }
  }

  class ByBuildId(@SerializedName("buildId") val buildId: Int) : PluginDescriptor() {
    override fun toString(): String {
      return "PluginDescriptor.ByBuildId(buildId=$buildId)"
    }
  }

  class ByFile(@SerializedName("file") val file: File) : PluginDescriptor() {
    constructor(path: String) : this(File(path))

    override fun toString(): String {
      return "PluginDescriptor.ByFile(file=$file)"
    }

  }

  //this class is not intended to be serialized yet.
  class ByInstance(@Transient val plugin: Plugin) : PluginDescriptor() {

    override fun toString(): String {
      return "PluginDescriptor.ByInstance(plugin=$plugin)"
    }
  }
}

sealed class IdeDescriptor() {

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

  class ByFile(@SerializedName("file") val file: File) : IdeDescriptor() {

    constructor(path: String) : this(File(path))

    override fun toString(): String {
      return "IdeDescriptor.ByFile(file=$file)"
    }

  }

  class ByVersion(@SerializedName("version") val version: IdeVersion) : IdeDescriptor() {
    override fun toString(): String {
      return "IdeDescriptor.ByVersion(version=$version)"
    }
  }

  //this class is not intended to be serialized yet.
  class ByInstance(@Transient val ide: Ide) : IdeDescriptor() {

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