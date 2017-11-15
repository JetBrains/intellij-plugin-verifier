package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import com.jetbrains.pluginverifier.repository.files.AvailableFile
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class LruSweepPolicy<K>(private val diskSpaceSetting: DiskSpaceSetting) : SweepPolicy<K> {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(LruSweepPolicy::class.java)
  }

  override fun selectKeysForDeletion(sweepInfo: SweepInfo<K>): List<K> {
    val availableSpace = diskSpaceSetting.maxSpaceUsage - sweepInfo.totalSpaceUsed
    if (availableSpace >= diskSpaceSetting.lowSpaceThreshold) {
      return emptyList()
    } else {
      LOG.info("It's time to remove unused plugins from cache. Download cache usage: ${sweepInfo.totalSpaceUsed.bytesToMegabytes()} Mb; " +
          "Estimated available space (Mb): ${availableSpace.bytesToMegabytes()}")

      val lastUsedToCandidate = TreeMap<Long, AvailableFile<K>>()

      for (availableFile in sweepInfo.availableFiles) {
        val usageStatistic = sweepInfo.usageStatistics[availableFile.key]!!
        lastUsedToCandidate[usageStatistic.lastAccessTime] = availableFile
      }

      val delete = arrayListOf<K>()
      var needSpace = diskSpaceSetting.lowSpaceThreshold - availableSpace
      for ((_, candidate) in lastUsedToCandidate) {
        if (needSpace > 0) {
          delete.add(candidate.key)
          needSpace -= candidate.size
        } else {
          break
        }
      }

      return delete
    }
  }

}