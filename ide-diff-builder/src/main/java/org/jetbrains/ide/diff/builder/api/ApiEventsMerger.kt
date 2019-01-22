package org.jetbrains.ide.diff.builder.api

/**
 * Utility class that merges history of API events, removing ambiguity.
 */
class ApiEventsMerger {
  fun mergeEvents(events: List<ApiEvent>): List<ApiEvent> {
    if (events.size <= 1) {
      return events
    }

    fun canFollowEachOther(one: ApiEvent, two: ApiEvent): Boolean {
      return when (one) {
        is IntroducedIn -> two is RemovedIn
        is RemovedIn -> two is IntroducedIn
      }
    }

    val sortedEvents = events.sortedBy { it.ideVersion }
    val result = arrayListOf<ApiEvent>()
    for (event in sortedEvents) {
      if (result.isEmpty() || canFollowEachOther(result.last(), event)) {
        result += event
      }
    }
    return result
  }

}