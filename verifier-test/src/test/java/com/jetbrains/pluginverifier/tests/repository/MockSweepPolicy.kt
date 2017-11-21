package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.SweepInfo
import com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy
import com.jetbrains.pluginverifier.repository.files.AvailableFile

class MockSweepPolicy : SweepPolicy<Int> {
  override fun isNecessary(totalSpaceUsed: SpaceAmount): Boolean = true

  override fun selectFilesForDeletion(sweepInfo: SweepInfo<Int>): List<AvailableFile<Int>> = emptyList()
}