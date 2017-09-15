package com.jetbrains.pluginverifier.repository

import org.apache.commons.io.FileUtils
import java.io.File

class FreeDiskSpaceWatcher(private val watchDir: File,
                           private val maximumByParameter: Long?,
                           private val lowSpaceThreshold: Long = FileUtils.ONE_GB,
                           private val enoughSpaceThreshold: Long = lowSpaceThreshold * 3) {

  fun getSpaceReport() = FreeSpaceReport(getSpaceUsage(), estimateAvailableSpace(), lowSpaceThreshold, enoughSpaceThreshold, watchDir)

  private fun getSpaceUsage() = FileUtils.sizeOfDirectory(watchDir)

  private fun estimateAvailableSpace(): Long? {
    val realUsage = getSpaceUsage()
    if (maximumByParameter != null) {
      return maximumByParameter - realUsage
    }

    val usableSpace = watchDir.usableSpace
    if (usableSpace != 0L) {
      return usableSpace
    }

    //Unable to estimate available space.
    return null
  }

}