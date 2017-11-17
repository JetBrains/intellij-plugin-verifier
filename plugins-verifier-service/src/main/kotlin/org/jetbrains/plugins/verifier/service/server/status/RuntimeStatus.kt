package org.jetbrains.plugins.verifier.service.server.status

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.bytesToSpaceAmount

data class MemoryInfo(val totalMemory: SpaceAmount,
                      val freeMemory: SpaceAmount,
                      val usedMemory: SpaceAmount,
                      val maxMemory: SpaceAmount)

fun getMemoryInfo(): MemoryInfo {
  val runtime = Runtime.getRuntime()
  return MemoryInfo(
      runtime.totalMemory().bytesToSpaceAmount(),
      runtime.freeMemory().bytesToSpaceAmount(),
      (runtime.totalMemory() - runtime.freeMemory()).bytesToSpaceAmount(),
      runtime.maxMemory().bytesToSpaceAmount()
  )
}