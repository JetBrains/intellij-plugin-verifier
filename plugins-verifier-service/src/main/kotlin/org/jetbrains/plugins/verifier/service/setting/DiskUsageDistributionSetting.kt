package org.jetbrains.plugins.verifier.service.setting

enum class DiskUsageDistributionSetting(val proportion: Double) {
  IDE_DOWNLOAD_DIR(0.4),
  PLUGIN_DOWNLOAD_DIR(0.4),
  OTHER_NEEDS(0.2);

  fun getAbsoluteDiskSpace(maximumDiskUsage: Long) =
      (maximumDiskUsage * proportion).toLong()

  companion object {
    init {
      require(DiskUsageDistributionSetting.values().sumByDouble { it.proportion } == 1.0)
    }
  }
}