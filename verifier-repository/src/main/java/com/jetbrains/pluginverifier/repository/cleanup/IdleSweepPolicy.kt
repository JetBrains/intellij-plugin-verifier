package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.files.AvailableFile

/**
 * [SweepPolicy] that doesn't sweep any files.
 */
class IdleSweepPolicy<T> : SweepPolicy<T> {
  override fun isNecessary(totalSpaceUsed: SpaceAmount) = false

  override fun selectFilesForDeletion(sweepInfo: SweepInfo<T>) = emptyList<AvailableFile<T>>()
}