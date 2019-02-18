package com.jetbrains.pluginverifier.reporting.downloading

import com.jetbrains.pluginverifier.misc.formatDuration
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import java.time.Duration

/**
 * Data about plugin downloading time and total size of the plugin.
 */
data class PluginDownloadReport(
    val pluginInfo: PluginInfo,
    val downloadDuration: Duration,
    val pluginSize: SpaceAmount
) {

  override fun toString() =
      pluginInfo.presentableName + " " + if (downloadDuration == Duration.ZERO) {
        "has been found locally ($pluginSize)"
      } else {
        "has been downloaded in ${downloadDuration.formatDuration()} ($pluginSize)"
      }

}