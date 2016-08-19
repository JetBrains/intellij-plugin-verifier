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
sealed class PluginDescriptor(@SerializedName("pluginId") val pluginId: String,
                              @SerializedName("version") val version: String) {

  abstract override fun equals(other: Any?): Boolean

  abstract override fun hashCode(): Int

  class ByUpdateInfo(@SerializedName("updateInfo") val updateInfo: UpdateInfo) : PluginDescriptor(updateInfo.pluginId, updateInfo.version) {
    override fun equals(other: Any?): Boolean = other is ByUpdateInfo && updateInfo.equals(other.updateInfo)

    override fun hashCode(): Int = updateInfo.hashCode()

    override fun toString(): String = "$updateInfo"
  }

  class ByXmlId(pluginId: String, version: String) : PluginDescriptor(pluginId, version) {
    override fun equals(other: Any?): Boolean = other is ByXmlId && pluginId.equals(other.pluginId) && version.equals(other.version)

    override fun hashCode(): Int = pluginId.hashCode() + version.hashCode()

    override fun toString(): String = "$pluginId:$version"
  }

  class ByFile(pluginId: String, version: String, @Transient val file: File) : PluginDescriptor(pluginId, version) {
    override fun toString(): String = "${file.name}"

    override fun equals(other: Any?): Boolean = other is ByFile && file.equals(other.file)

    override fun hashCode(): Int = file.hashCode()

  }

  class ByInstance(@Transient val plugin: Plugin) : PluginDescriptor(plugin.pluginId!!, plugin.pluginVersion!!) {
    override fun equals(other: Any?): Boolean = other is ByInstance && plugin.equals(other.plugin)

    override fun hashCode(): Int = plugin.hashCode()

    override fun toString(): String = "$plugin"
  }
}

sealed class IdeDescriptor(@SerializedName("version") val ideVersion: IdeVersion) {


  abstract override fun equals(other: Any?): Boolean

  abstract override fun hashCode(): Int

  object AnyIde : IdeDescriptor(IdeVersion.createIdeVersion("0")) {
    override fun equals(other: Any?): Boolean = other is AnyIde

    override fun hashCode(): Int = "AnyIde".hashCode()

    override fun toString(): String = "AnyIde"
  }

  class ByFile(ideVersion: IdeVersion, @Transient val file: File) : IdeDescriptor(ideVersion) {
    override fun equals(other: Any?): Boolean = other is ByFile && file.equals(other.file)

    override fun hashCode(): Int = file.hashCode()

    override fun toString(): String = "${file.name}"

  }

  class ByVersion(ideVersion: IdeVersion) : IdeDescriptor(ideVersion) {
    override fun equals(other: Any?): Boolean = other is ByVersion && ideVersion.equals(other.ideVersion)

    override fun hashCode(): Int = ideVersion.hashCode()

    override fun toString(): String = "$ideVersion"
  }

  class ByInstance(@Transient val ide: Ide, @Transient val ideResolver: Resolver? = null) : IdeDescriptor(ide.version) {
    override fun equals(other: Any?): Boolean = other is ByInstance && ideVersion.equals(other.ideVersion) && ide.equals(other.ide)

    override fun hashCode(): Int = ide.hashCode()

    override fun toString(): String = "$ide"
  }
}

sealed class JdkDescriptor() {

  abstract override fun equals(other: Any?): Boolean

  abstract override fun hashCode(): Int

  class ByFile(@Transient val file: File) : JdkDescriptor() {
    constructor(path: String) : this(File(path))

    override fun equals(other: Any?): Boolean = other is ByFile && file.equals(other.file)

    override fun hashCode(): Int = file.hashCode()

    override fun toString(): String = "${file.name}"
  }


}