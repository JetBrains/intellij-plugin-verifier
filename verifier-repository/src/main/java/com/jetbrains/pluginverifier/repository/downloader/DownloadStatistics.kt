package com.jetbrains.pluginverifier.repository.downloader

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Accumulates downloading events and provides statistics.
 */
class DownloadStatistics {

  private val events = Collections.synchronizedList(arrayListOf<DownloadEvent>())

  @Synchronized
  private fun reportEvent(startInstant: Instant, endInstant: Instant, downloadedAmount: SpaceAmount) {
    events += DownloadEvent(startInstant, endInstant, downloadedAmount)
  }

  fun getTotalDownloadedAmount(): SpaceAmount {
    return events.fold(SpaceAmount.ZERO_SPACE) { acc, event ->
      acc + event.amount
    }
  }

  fun getTotalAstronomicalDownloadDuration(): Duration {
    //Use sweep line algorithm, because events may intersect.
    val startEvents = events.groupBy { it.startInstant }
    val endEvents = events.groupBy { it.endInstant }
    val allInstants = (events.map { it.startInstant } + events.map { it.endInstant }).sorted()
    var totalDuration = Duration.ZERO
    val activeEvents = hashSetOf<DownloadEvent>()
    for (i in 0 until allInstants.size - 1) {
      val segmentStart = allInstants[i]
      val segmentEnd = allInstants[i + 1]

      activeEvents -= endEvents[segmentStart].orEmpty()
      activeEvents += startEvents[segmentStart].orEmpty()

      if (activeEvents.isNotEmpty()) {
        totalDuration += Duration.between(segmentStart, segmentEnd)
      }
    }
    return totalDuration
  }

  fun downloadStarted(): DownloadBlock {
    val startInstant = Instant.now()
    return DownloadBlock(startInstant)
  }

  inner class DownloadBlock(private val startInstant: Instant) {
    fun downloadEnded(downloadedAmount: SpaceAmount) {
      reportEvent(startInstant, Instant.now(), downloadedAmount)
    }
  }
}