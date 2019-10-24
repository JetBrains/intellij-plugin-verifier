package com.jetbrains.pluginverifier.repository.cleanup

import java.time.Instant

/**
 * Recording of the usage statistics of a resource
 * that can be useful to sort the resources by their importance.
 */
data class UsageStatistic(
  /**
   * The last time this resource was added or locked
   */
  var lastAccessTime: Instant,
  /**
   * The total number of times this resource was accessed
   */
  var timesAccessed: Long
) {

  override fun toString() = "Times accessed: $timesAccessed; Last access time: $lastAccessTime"
}