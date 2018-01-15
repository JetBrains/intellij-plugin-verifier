package org.jetbrains.plugins.verifier.service.setting

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount

/**
 * The distribution of the disk space among the services and system
 * run on the server.
 *
 * Currently, the space is distributed as follows:
 * - 40% for the downloaded IDEs,
 * - 40% for the downloaded plugins,
 * - 20% for other needs, such as temp files.
 */
enum class DiskUsageDistributionSetting(val proportion: Double) {
  IDE_DOWNLOAD_DIR(0.4),
  PLUGIN_DOWNLOAD_DIR(0.4),
  OTHER_NEEDS(0.2);

  fun getIntendedSpace(maximumDiskUsage: SpaceAmount) = maximumDiskUsage * proportion

  companion object {
    init {
      require(DiskUsageDistributionSetting.values().sumByDouble { it.proportion } == 1.0)
    }
  }
}