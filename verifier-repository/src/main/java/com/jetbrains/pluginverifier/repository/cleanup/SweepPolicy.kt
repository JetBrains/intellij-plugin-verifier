package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.files.AvailableFile

/**
 * Sweep policy determines when the cleanup procedure must be carried out
 * and which files should be removed.
 */
interface SweepPolicy<K> {

  /**
   * Determines whether it is necessary to carry out the cleanup procedure at the moment.
   * This method can be called often, so it is expected to return quickly.
   */
  fun isNecessary(totalSpaceUsed: SpaceAmount): Boolean

  /**
   * Given the current state of the [file repository] [com.jetbrains.pluginverifier.repository.files.FileRepository]
   * determines which files must be removed.
   */
  fun selectFilesForDeletion(sweepInfo: SweepInfo<K>): List<AvailableFile<K>>
}