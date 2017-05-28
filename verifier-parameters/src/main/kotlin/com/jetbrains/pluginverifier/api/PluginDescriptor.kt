package com.jetbrains.pluginverifier.api

import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.repository.UpdateInfo
import java.io.Closeable
import java.io.File

sealed class PluginDescriptor : Closeable {

  abstract val presentableName: String

  final override fun toString(): String = presentableName

  data class ByUpdateInfo(val updateInfo: UpdateInfo) : PluginDescriptor() {
    override val presentableName: String = updateInfo.toString()

    override fun close() = Unit
  }

  data class ByFile(val pluginFile: File) : PluginDescriptor() {
    override val presentableName: String = pluginFile.toString()

    override fun close() = Unit
  }

  data class ByInstance(val createOk: CreatePluginResult.OK) : PluginDescriptor() {
    override val presentableName: String = createOk.plugin.toString()

    override fun close() = createOk.close()
  }
}