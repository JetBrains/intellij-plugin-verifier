package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import java.io.File

data class FreeSpaceReport(val usedSpace: Long,
                           val availableSpace: Long,
                           val watchedDirectory: File,
                           val diskSpaceSetting: DiskSpaceSetting) {

  override fun toString(): String = buildString {
    append("$watchedDirectory: ")
    append("${availableSpace.bytesToMegabytes()} Mb available; ")
    append("${diskSpaceSetting.lowSpaceThreshold.bytesToMegabytes()} Mb desired; ")
    append("${usedSpace.bytesToMegabytes()} Mb occupied;")
  }

}