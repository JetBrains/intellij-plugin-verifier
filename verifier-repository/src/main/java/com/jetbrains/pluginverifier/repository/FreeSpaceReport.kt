package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import java.io.File

data class FreeSpaceReport(val usedSpace: Long,
                           val availableSpace: Long,
                           val lowSpaceThreshold: Long,
                           val enoughSpaceThreshold: Long,
                           val watchedDirectory: File) {

  override fun toString(): String = buildString {
    append("$watchedDirectory: ")
    append("${availableSpace.bytesToMegabytes()} Mb available; ")
    append("${lowSpaceThreshold.bytesToMegabytes()} Mb desired; ")
    append("${usedSpace.bytesToMegabytes()} Mb occupied;")
  }

}