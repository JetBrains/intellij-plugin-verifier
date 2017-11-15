package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.cleanup.SweepInfo
import com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy
import com.jetbrains.pluginverifier.repository.files.AvailableFile

class MockSweepPolicy : SweepPolicy<Int> {
  override fun selectFilesForDeletion(sweepInfo: SweepInfo<Int>): List<AvailableFile<Int>> = emptyList()
}