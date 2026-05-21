/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.cache

import java.util.concurrent.atomic.AtomicLong

/**
 * Aggregate hit/miss statistics for a single cache.
 *
 * A `hit` is a lookup that was served entirely from already-cached state.
 * A `miss` is a lookup that had to invoke the underlying [com.jetbrains.pluginverifier.repository.provider.ResourceProvider]
 * (or wait for another thread doing the same).
 * A `failure` is a lookup that ended in a `NotFound`/`Failed` outcome — the cache did not retain anything.
 * An `eviction` is a previously-stored resource that was removed by the eviction policy.
 *
 * All counters are updated atomically and may be read concurrently.
 */
class CacheStatistics {
  private val hitCount = AtomicLong()
  private val missCount = AtomicLong()
  private val failureCount = AtomicLong()
  private val evictionCount = AtomicLong()

  fun recordHit() {
    hitCount.incrementAndGet()
  }

  fun recordMiss() {
    missCount.incrementAndGet()
  }

  fun recordFailure() {
    failureCount.incrementAndGet()
  }

  fun recordEviction() {
    evictionCount.incrementAndGet()
  }

  fun recordEvictions(count: Int) {
    if (count > 0) {
      evictionCount.addAndGet(count.toLong())
    }
  }

  fun hits(): Long = hitCount.get()
  fun misses(): Long = missCount.get()
  fun failures(): Long = failureCount.get()
  fun evictions(): Long = evictionCount.get()

  fun total(): Long = hits() + misses() + failures()

  /**
   * Hit rate as a fraction in `[0.0, 1.0]`. Returns `0.0` if the cache has not been accessed yet.
   */
  fun hitRate(): Double {
    val totalLookups = hits() + misses() + failures()
    return if (totalLookups == 0L) 0.0 else hits().toDouble() / totalLookups
  }

  /**
   * One-line, human-readable summary suitable for `info`-level logging.
   */
  fun presentableSummary(): String {
    val total = total()
    val hitRatePercent = if (total == 0L) "n/a" else "%.1f%%".format(hitRate() * 100.0)
    return "hits=${hits()}, misses=${misses()}, failures=${failures()}, evictions=${evictions()}, total=$total, hit-rate=$hitRatePercent"
  }
}
