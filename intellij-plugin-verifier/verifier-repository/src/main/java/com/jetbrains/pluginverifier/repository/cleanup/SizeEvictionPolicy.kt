package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.resources.AvailableResource
import com.jetbrains.pluginverifier.repository.resources.EvictionInfo
import com.jetbrains.pluginverifier.repository.resources.EvictionPolicy

/**
 * Eviction policy that maintains the [maximum] [maximumSize]
 * size of the [repository] [com.jetbrains.pluginverifier.repository.resources.ResourceRepository].
 *
 * It selects for deletion resources that have not been
 * accessed for the longest time.
 * If the last times are equal, the resources are compared by number of access times.
 */
class SizeEvictionPolicy<R, K>(private val maximumSize: Int) : EvictionPolicy<R, K, SizeWeight> {
  override fun isNecessary(totalWeight: SizeWeight) = totalWeight.size > maximumSize

  override fun selectResourcesForEviction(evictionInfo: EvictionInfo<R, K, SizeWeight>) =
      evictionInfo.availableResources
          .filterNot { it.isLocked }
          .sortedWith(
              //Firstly remove resources that haven't been accessed for the longest time.
              //Then, if they were accessed at the same time, compare by number of accesses
              compareBy<AvailableResource<R, K, SizeWeight>> {
                it.usageStatistic.lastAccessTime
              }.thenBy {
                it.usageStatistic.timesAccessed
              }.thenBy {
                //As the last resort, compare by keys, to have consistent results.
                if (it.key is Comparable<*>) {
                  it.key
                } else {
                  it.toString()
                }
              }
          )
          .take(howManyToRemove(evictionInfo))

  private fun howManyToRemove(evictionInfo: EvictionInfo<R, K, SizeWeight>): Int {
    val size = evictionInfo.totalWeight.size
    return (size - maximumSize).coerceAtLeast(0L).toInt()
  }
}