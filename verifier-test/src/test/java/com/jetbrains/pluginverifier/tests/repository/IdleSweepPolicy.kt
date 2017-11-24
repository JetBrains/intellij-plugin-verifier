package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.SweepInfo
import com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy
import com.jetbrains.pluginverifier.repository.files.AvailableFile

object IdleSweepPolicy : SweepPolicy<Int> {
  override fun isNecessary(totalSpaceUsed: SpaceAmount): Boolean = false

  override fun selectFilesForDeletion(sweepInfo: SweepInfo<Int>): List<AvailableFile<Int>> = emptyList()
}