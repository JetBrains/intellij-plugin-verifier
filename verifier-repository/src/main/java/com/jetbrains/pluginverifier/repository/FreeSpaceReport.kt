package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import java.io.File

data class FreeSpaceReport(val usedSpace: Long,
                           val estimatedAvailableSpace: Long?,
                           val lowSpaceThreshold: Long,
                           val enoughSpaceThreshold: Long,
                           val watchedDirectory: File) {

  override fun toString(): String = buildString {
    append("Directory $watchedDirectory space: ")
    if (estimatedAvailableSpace != null) {
      append("${estimatedAvailableSpace.bytesToMegabytes()} Mb available; ")
    } else {
      append("unknown amount of available space; ")
    }
    append("${lowSpaceThreshold.bytesToMegabytes()} Mb desired;")
    append("${usedSpace.bytesToMegabytes()} Mb occupied;")
  }

}