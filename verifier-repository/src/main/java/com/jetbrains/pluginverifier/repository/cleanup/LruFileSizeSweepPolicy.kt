package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.files.AvailableFile

/**
 * The sweep policy which selects the files based on their last access time.
 * If multiple files have the same last access time, the heaviest one is chosen.
 *
 * The policy selects as many files as necessary until the disk usage corresponds to [diskSpaceSetting].
 */
class LruFileSizeSweepPolicy<K>(private val diskSpaceSetting: DiskSpaceSetting) : SweepPolicy<K> {

  private fun estimateFreeSpaceAmount(totalSpaceUsed: SpaceAmount) =
      diskSpaceSetting.maxSpaceUsage - totalSpaceUsed

  override fun isNecessary(totalSpaceUsed: SpaceAmount): Boolean =
      estimateFreeSpaceAmount(totalSpaceUsed) < diskSpaceSetting.lowSpaceThreshold

  private val lruHeaviestFilesComparator = compareBy<AvailableFile<K>> { it.usageStatistic.lastAccessTime }
      .thenByDescending { it.fileInfo.size }
      .thenBy { it.fileInfo.file.name }

  override fun selectFilesForDeletion(sweepInfo: SweepInfo<K>): List<AvailableFile<K>> {
    if (isNecessary(sweepInfo.totalSpaceUsed)) {
      val freeFiles = sweepInfo.availableFiles.filterNot { it.isLocked }
      if (freeFiles.isNotEmpty()) {

        val sortedCandidates = sweepInfo.availableFiles.sortedWith(lruHeaviestFilesComparator)
        val deleteFiles = arrayListOf<AvailableFile<K>>()
        val estimatedFreeSpaceAmount = estimateFreeSpaceAmount(sweepInfo.totalSpaceUsed)
        var needToFreeSpace = diskSpaceSetting.minimumFreeSpaceAfterCleanup - estimatedFreeSpaceAmount
        for (candidate in sortedCandidates) {
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