package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.cleanup.SweepInfo
import com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy

class MockSweepPolicy : SweepPolicy<Int> {
  override fun selectKeysForDeletion(sweepInfo: SweepInfo<Int>): List<Int> = emptyList()
}