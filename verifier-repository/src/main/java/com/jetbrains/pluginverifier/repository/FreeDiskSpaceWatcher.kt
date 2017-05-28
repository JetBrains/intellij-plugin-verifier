package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class FreeDiskSpaceWatcher(val watchDir: File, val maximumByParameterMb: Long?) {

  companion object {
    private val ONE_GIGABYTE = 1024L

    private val EXPECTED_SPACE = 3 * ONE_GIGABYTE

    private val LOW_THRESHOLD = ONE_GIGABYTE
  }

  fun getSpaceUsageMb() = FileUtils.sizeOfDirectory(watchDir).bytesToMegabytes()

  fun estimateAvailableSpace(): Long {
    val realUsageMb = getSpaceUsageMb()
    if (maximumByParameterMb != null) {
      return maximumByParameterMb - realUsageMb
    }

    val askedSpace = watchDir.usableSpace.bytesToMegabytes()
    if (askedSpace != 0L) {
      return askedSpace
    }
    return EXPECTED_SPACE
  }

  fun isLowSpace(): Boolean {
    val usedSpace = getSpaceUsageMb()
    val availableSpace = estimateAvailableSpace()
    if (availableSpace < LOW_THRESHOLD) {
      DownloadManager.LOG.warn("Cache directory ${RepositoryConfiguration.downloadDir} has only $availableSpace < $LOW_THRESHOLD Mb; occupied = $usedSpace Mb")
      return true
    } else {
      return false
    }
  }
}