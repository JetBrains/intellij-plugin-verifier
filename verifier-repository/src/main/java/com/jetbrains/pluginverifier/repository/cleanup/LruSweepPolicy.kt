package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.files.AvailableFile
import java.time.Instant
import java.util.*

class LruSweepPolicy<K>(private val diskSpaceSetting: DiskSpaceSetting) : SweepPolicy<K> {

  override fun selectFilesForDeletion(sweepInfo: SweepInfo<K>): List<AvailableFile<K>> {
    val availableSpace = diskSpaceSetting.maxSpaceUsage - sweepInfo.totalSpaceUsed
    if (availableSpace >= diskSpaceSetting.lowSpaceThreshold) {
      return emptyList()
    } else {
      val lastUsedToCandidate = TreeMap<Instant, AvailableFile<K>>()

      for (availableFile in sweepInfo.availableFiles) {
        val usageStatistic = sweepInfo.usageStatistics[availableFile.key]!!
        lastUsedToCandidate[usageStatistic.lastAccessTime] = availableFile
      }

      val delete = arrayListOf<AvailableFile<K>>()
      var needSpace = diskSpaceSetting.lowSpaceThreshold - availableSpace
      for ((_, candidate) in lastUsedToCandidate) {
        if (needSpace > 0) {
          delete.add(candidate)
          needSpace -= candidate.size
        } else {
          break
        }
      }

      return delete
    }
  }

}