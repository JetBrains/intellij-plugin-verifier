package com.jetbrains.pluginverifier.api

import com.jetbrains.pluginverifier.repository.UpdateInfo
import java.io.File

sealed class PluginCoordinate {

  abstract val presentableName: String

  final override fun toString(): String = presentableName

  data class ByUpdateInfo(val updateInfo: UpdateInfo) : PluginCoordinate() {
    override val presentableName: String = updateInfo.toString()
  }

  data class ByFile(val pluginFile: File) : PluginCoordinate() {
    override val presentableName: String = pluginFile.toString()
  }

}