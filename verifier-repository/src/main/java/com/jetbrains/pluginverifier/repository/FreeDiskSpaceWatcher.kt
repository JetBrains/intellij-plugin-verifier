package com.jetbrains.pluginverifier.repository

import org.apache.commons.io.FileUtils
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class FreeDiskSpaceWatcher(val watchDir: File, val maximumByParameter: Long?) {

  companion object {

    private val LOW_THRESHOLD = FileUtils.ONE_GB

    private val ENOUGH_SPACE = LOW_THRESHOLD * 3
  }

  fun getSpaceUsage() = FileUtils.sizeOfDirectory(watchDir)

  fun isEnoughSpace(): Boolean {
    val estimatedSpace = estimateAvailableSpace()
    return estimatedSpace == null || estimatedSpace > ENOUGH_SPACE
  }

  fun estimateAvailableSpace(): Long? {
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

  fun isLittleSpace(): Boolean {
    val usedSpace = getSpaceUsage()
    val estimatedSpace = estimateAvailableSpace() ?: return false //Unable to evaluate available space => assume it's enough.
    if (estimatedSpace < LOW_THRESHOLD) {
      DownloadManager.LOG.warn("Cache directory ${RepositoryConfiguration.downloadDir} has only $estimatedSpace < $LOW_THRESHOLD bytes; occupied = $usedSpace bytes")
      return true
    } else {
      return false
    }
  }
}