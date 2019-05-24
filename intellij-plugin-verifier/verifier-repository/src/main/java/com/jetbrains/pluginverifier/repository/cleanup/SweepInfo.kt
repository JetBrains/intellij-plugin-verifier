package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.downloader.SpaceWeight
import com.jetbrains.pluginverifier.repository.files.AvailableFile
import com.jetbrains.pluginverifier.repository.resources.EvictionInfo
import java.nio.file.Path

/**
 * Aggregates information on the current state of
 * the [repository] [com.jetbrains.pluginverifier.repository.files.FileRepository].
 * This information is used by the [SweepPolicy] to determine the set
 * of files to be deleted on the cleanup procedure.
 */
data class SweepInfo<out K>(private val evictionInfo: EvictionInfo<Path, K, SpaceWeight>) {
  /**
   * The total amount of disk space used at the moment
   */
  val totalSpaceUsed: SpaceAmount
    get() = evictionInfo.totalWeight.spaceAmount

  /**
   * All the currently available files
   */
  val availableFiles: List<AvailableFile<K>> by lazy {
    evictionInfo.availableResources.map {
      with(it) {
        AvailableFile(key, resourceInfo, usageStatistic, isLocked)
      }
    }
  }

}