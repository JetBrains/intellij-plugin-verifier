package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.resources.EvictionInfo
import com.jetbrains.pluginverifier.repository.resources.EvictionPolicy
import com.jetbrains.pluginverifier.repository.resources.ResourceWeight

/**
 * Eviction policy that maintains the [maximum] [maximumSize]
 * size of the [repository] [com.jetbrains.pluginverifier.repository.resources.ResourceRepository].
 *
 * It selects for deletion the files that have not been
 * accessed for the longest time.
 */
class SizeEvictionPolicy<R, K>(val maximumSize: Long) : EvictionPolicy<R, K> {
  override fun isNecessary(totalWeight: ResourceWeight) =
      (totalWeight as SizeWeight).size > maximumSize

  override fun selectResourcesForDeletion(evictionInfo: EvictionInfo<R, K>) =
      evictionInfo.availableResources
          .filterNot { it.isLocked }
          .sortedBy { it.usageStatistic.lastAccessTime }
          .take(((evictionInfo.totalWeight as SizeWeight).size - maximumSize).coerceAtLeast(0L).toInt())
}