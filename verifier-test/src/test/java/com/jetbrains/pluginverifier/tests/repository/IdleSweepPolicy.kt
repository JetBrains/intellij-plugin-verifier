package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.SweepInfo
import com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy
import com.jetbrains.pluginverifier.repository.files.AvailableFile

class IdleSweepPolicy<T> : SweepPolicy<T> {
  override fun isNecessary(totalSpaceUsed: SpaceAmount): Boolean = false

  override fun selectFilesForDeletion(sweepInfo: SweepInfo<T>): List<AvailableFile<T>> = emptyList()
}