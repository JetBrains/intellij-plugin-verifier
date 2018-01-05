package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.misc.nameWithoutExtension
import com.jetbrains.pluginverifier.parameters.filtering.PluginIdAndVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.local.createLocalPluginInfo
import java.nio.file.Path

sealed class PluginCoordinate {

  abstract val presentableName: String

  abstract val fileFinder: PluginFileFinder

  final override fun toString(): String = presentableName

  data class ByUpdateInfo(val updateInfo: UpdateInfo, val pluginRepository: PluginRepository) : PluginCoordinate() {
    override val presentableName: String = updateInfo.toString()

    override val fileFinder: PluginFileFinder
      get() = RepositoryPluginFileFinder(pluginRepository, updateInfo)

  }

  data class ByFile(val pluginFile: Path) : PluginCoordinate() {
    override val presentableName: String = pluginFile.toString()

    override val fileFinder: PluginFileFinder
      get() = LocalFileFinder(pluginFile)

  }

}

private fun guessPluginIdAndVersion(pluginFile: Path): PluginIdAndVersion {
  val name = pluginFile.nameWithoutExtension
  val version = name.substringAfterLast('-')
  return PluginIdAndVersion(name.substringBeforeLast('-'), version)
}

fun PluginCoordinate.createPluginInfo(pluginDetailsProvider: PluginDetailsProvider): PluginInfo = when (this) {
  is PluginCoordinate.ByUpdateInfo -> updateInfo
  is PluginCoordinate.ByFile -> {
    pluginDetailsProvider.providePluginDetails(this).use { pluginDetails ->
      val plugin = pluginDetails.plugin
      return if (plugin != null) {
        createLocalPluginInfo(pluginFile, plugin)
      } else {
        val (pluginId, version) = guessPluginIdAndVersion(pluginFile)
        PluginInfo(pluginId, version, pluginFile.toUri().toURL())
      }
    }
  }
}

fun PluginCoordinate.toPluginIdAndVersion(pluginDetailsProvider: PluginDetailsProvider): PluginIdAndVersion {
  val pluginInfo = createPluginInfo(pluginDetailsProvider)
  return PluginIdAndVersion(pluginInfo.pluginId, pluginInfo.version)
}
