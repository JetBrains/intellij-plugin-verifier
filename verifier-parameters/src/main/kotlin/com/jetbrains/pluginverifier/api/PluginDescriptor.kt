package com.jetbrains.pluginverifier.api

import com.jetbrains.pluginverifier.repository.UpdateInfo
import java.io.File

sealed class PluginDescriptor {

  abstract val presentableName: String

  final override fun toString(): String = presentableName

  data class ByUpdateInfo(val updateInfo: UpdateInfo) : PluginDescriptor() {
    override val presentableName: String = updateInfo.toString()
  }

  data class ByFile(val pluginFile: File) : PluginDescriptor() {
    override val presentableName: String = pluginFile.toString()
  }

}