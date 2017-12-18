package com.jetbrains.pluginverifier.misc

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.bytesToSpaceAmount

/**
 * Memory info aggregates information on the memory used.
 */
data class MemoryInfo(val totalMemory: SpaceAmount,
                      val freeMemory: SpaceAmount,
                      val usedMemory: SpaceAmount,
                      val maxMemory: SpaceAmount) {

  companion object {
    fun getRuntimeMemoryInfo() = with(Runtime.getRuntime()) {
      MemoryInfo(
          totalMemory().bytesToSpaceAmount(),
          freeMemory().bytesToSpaceAmount(),
          (totalMemory() - freeMemory()).bytesToSpaceAmount(),
          maxMemory().bytesToSpaceAmount()
      )
    }
  }
}