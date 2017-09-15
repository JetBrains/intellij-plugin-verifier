package com.jetbrains.pluginverifier.repository

import org.apache.commons.io.FileUtils
import java.io.File

class FreeDiskSpaceWatcher(private val watchDir: File,
                           private val maxSpaceUsage: Long,
                           private val lowSpaceThreshold: Long = FileUtils.ONE_GB,
                           private val enoughSpaceThreshold: Long = lowSpaceThreshold * 3) {

  fun getSpaceReport() = FreeSpaceReport(getSpaceUsage(), getAvailableSpace(), lowSpaceThreshold, enoughSpaceThreshold, watchDir)

  private fun getSpaceUsage() = FileUtils.sizeOfDirectory(watchDir)

  private fun getAvailableSpace(): Long {
    val realUsage = getSpaceUsage()
    return maxSpaceUsage - realUsage
  }

}