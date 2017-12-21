package com.jetbrains.pluginverifier.misc

import java.time.Duration
import java.time.Instant
import java.time.Instant.now

/**
 * Stopwatch is a class that allows to track
 * periods of time of [duration].
 */
class Stopwatch(val duration: Duration) {

  private var lastInstant: Instant? = null

  @Synchronized
  fun isCycle(): Boolean = lastInstant == null || now().minus(duration).isAfter(lastInstant)

  @Synchronized
  fun reset() {
    lastInstant = now()
  }

}