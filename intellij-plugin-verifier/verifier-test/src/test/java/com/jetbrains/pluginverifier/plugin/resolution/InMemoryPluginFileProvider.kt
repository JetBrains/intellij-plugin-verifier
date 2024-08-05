package com.jetbrains.pluginverifier.plugin.resolution

import com.jetbrains.pluginverifier.plugin.PluginFileProvider
import com.jetbrains.pluginverifier.plugin.PluginFileProvider.Result.Found
import com.jetbrains.pluginverifier.plugin.PluginFileProvider.Result.NotFound
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.IdleFileLock
import java.nio.file.Path
import java.util.*

class InMemoryPluginFileProvider : PluginFileProvider {
  private val mapping = TreeMap<PluginInfo, Path>(pluginInfoComparator)

  override fun getPluginFile(pluginInfo: PluginInfo) =
    mapping[pluginInfo]
      ?.let { Found(IdleFileLock(it)) }
      ?: NotFound("Not found ${pluginInfo.fqn()}.")

  operator fun set(pluginInfo: PluginInfo, path: Path) {
    mapping[pluginInfo] = path
  }

  private val pluginInfoComparator: Comparator<PluginInfo>
    get() {
      return Comparator { info1, info2 ->
        val i1 = info1.fqn()
        val i2 = info2.fqn()
        i1.compareTo(i2)
      }
    }

  private fun PluginInfo?.fqn(): String {
    if (this == null) return ""
    return "${pluginId}:${version}"
  }
}