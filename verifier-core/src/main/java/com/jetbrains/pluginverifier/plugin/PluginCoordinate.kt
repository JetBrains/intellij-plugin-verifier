package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import java.io.File

sealed class PluginCoordinate {

  abstract val presentableName: String

  abstract val fileFinder: PluginFileFinder

  final override fun toString(): String = presentableName

  data class ByUpdateInfo(val updateInfo: UpdateInfo, val pluginRepository: PluginRepository) : PluginCoordinate() {
    override val presentableName: String = updateInfo.toString()

    override val fileFinder: PluginFileFinder
      get() = RepositoryPluginFileFinder(pluginRepository, updateInfo)

  }

  data class ByFile(val pluginFile: File) : PluginCoordinate() {
    override val presentableName: String = pluginFile.toString()

    override val fileFinder: PluginFileFinder
      get() = LocalFileFinder(pluginFile)

  }

}

fun PluginCoordinate.toPluginIdAndVersion(pluginDetailsProvider: PluginDetailsProvider): PluginIdAndVersion? = when (this) {
  is PluginCoordinate.ByUpdateInfo -> PluginIdAndVersion(updateInfo.pluginId, updateInfo.version)
  is PluginCoordinate.ByFile -> {
    pluginDetailsProvider.providePluginDetails(this).use { pluginDetails ->
      val plugin = pluginDetails.plugin
      return plugin?.let { PluginIdAndVersion(it.pluginId ?: "", it.pluginVersion ?: "") }
    }
  }
}
