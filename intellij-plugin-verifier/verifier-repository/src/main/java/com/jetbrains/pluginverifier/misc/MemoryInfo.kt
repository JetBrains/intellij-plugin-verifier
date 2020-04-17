/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.misc

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.bytesToSpaceAmount

/**
 * Memory info aggregates information on the memory used.
 */
data class MemoryInfo(
  val totalMemory: SpaceAmount,
  val freeMemory: SpaceAmount,
  val usedMemory: SpaceAmount,
  val maxMemory: SpaceAmount
) {

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

  override fun toString() = "Total memory: " + totalMemory + "; " +
    "Free memory: " + freeMemory + "; " +
    "Used memory: " + usedMemory + "; " +
    "Max memory: " + maxMemory + "; "

}