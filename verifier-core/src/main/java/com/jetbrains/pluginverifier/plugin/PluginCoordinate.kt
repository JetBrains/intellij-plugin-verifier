package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import java.io.File

sealed class PluginCoordinate {

  abstract val uniqueId: String

  abstract val presentableName: String

  abstract val fileFinder: PluginFileFinder

  final override fun toString(): String = presentableName

  data class ByUpdateInfo(val updateInfo: UpdateInfo, val pluginRepository: PluginRepository) : PluginCoordinate() {
    override val presentableName: String = updateInfo.toString()

    override val fileFinder: PluginFileFinder
      get() = RepositoryPluginFileFinder(pluginRepository, updateInfo)

    override val uniqueId: String = "#${updateInfo.updateId}"
  }

  data class ByFile(val pluginFile: File) : PluginCoordinate() {
    override val presentableName: String = pluginFile.toString()

    override val fileFinder: PluginFileFinder
      get() = LocalFileFinder(pluginFile)

    override val uniqueId: String = pluginFile.name
  }

}