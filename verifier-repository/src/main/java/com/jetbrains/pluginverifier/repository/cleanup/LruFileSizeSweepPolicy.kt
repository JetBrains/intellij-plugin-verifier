package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.files.AvailableFile
import java.time.Instant
import java.util.*

class LruFileSizeSweepPolicy<K>(private val diskSpaceSetting: DiskSpaceSetting) : SweepPolicy<K> {

  private fun estimateFreeSpaceAmount(totalSpaceUsed: SpaceAmount) =
      diskSpaceSetting.maxSpaceUsage - totalSpaceUsed

  private fun estimateNeedToFreeSpace(availableSpace: SpaceAmount) =
      diskSpaceSetting.lowSpaceThreshold * 2 - availableSpace

  override fun isNecessary(totalSpaceUsed: SpaceAmount): Boolean =
      estimateFreeSpaceAmount(totalSpaceUsed) < diskSpaceSetting.lowSpaceThreshold

  override fun selectFilesForDeletion(sweepInfo: SweepInfo<K>): List<AvailableFile<K>> {
    if (isNecessary(sweepInfo.totalSpaceUsed)) {
      val freeFiles = sweepInfo.availableFiles.filterNot { it.isLocked }
      if (freeFiles.isNotEmpty()) {
        val lastUsedToCandidate = TreeMap<Instant, AvailableFile<K>>()

        for (freeFile in freeFiles) {
          lastUsedToCandidate[freeFile.usageStatistic.lastAccessTime] = freeFile
        }

        val deleteFiles = arrayListOf<AvailableFile<K>>()
        val estimatedFreeSpaceAmount = estimateFreeSpaceAmount(sweepInfo.totalSpaceUsed)
        var needToFreeSpace = estimateNeedToFreeSpace(estimatedFreeSpaceAmount)
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

}