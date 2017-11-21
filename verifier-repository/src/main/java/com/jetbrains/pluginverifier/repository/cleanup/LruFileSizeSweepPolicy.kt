package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.files.AvailableFile
import java.time.Instant
import java.util.*

class LruFileSizeSweepPolicy<K>(private val diskSpaceSetting: DiskSpaceSetting) : SweepPolicy<K> {

  override fun selectFilesForDeletion(sweepInfo: SweepInfo<K>): List<AvailableFile<K>> {
    val availableSpace = diskSpaceSetting.maxSpaceUsage - sweepInfo.totalSpaceUsed
    if (availableSpace >= diskSpaceSetting.lowSpaceThreshold) {
      return emptyList()
    } else {
      val freeFiles = sweepInfo.availableFiles.filterNot { it.isLocked }
      if (freeFiles.isNotEmpty()) {
        val lastUsedToCandidate = TreeMap<Instant, AvailableFile<K>>()

        for (freeFile in freeFiles) {
          lastUsedToCandidate[freeFile.usageStatistic.lastAccessTime] = freeFile
        }

        val deleteFiles = arrayListOf<AvailableFile<K>>()
        var needToFreeSpace = estimateNeedToFreeSpace(availableSpace)
        for ((_, candidate) in lastUsedToCandidate) {
          if (needToFreeSpace > SpaceAmount.ZERO_SPACE) {
            deleteFiles.add(candidate)
            needToFreeSpace -= candidate.fileInfo.size
          } else {
            break
          }
        }

        return deleteFiles
      }
    }
    return emptyList()
  }

  private fun estimateNeedToFreeSpace(availableSpace: SpaceAmount) =
      diskSpaceSetting.lowSpaceThreshold * 2 - availableSpace

}