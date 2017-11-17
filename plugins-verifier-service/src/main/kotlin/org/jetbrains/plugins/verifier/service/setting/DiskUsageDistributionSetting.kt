package org.jetbrains.plugins.verifier.service.setting

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount

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