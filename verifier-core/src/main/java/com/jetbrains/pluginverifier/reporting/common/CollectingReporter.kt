package com.jetbrains.pluginverifier.reporting.common

import com.jetbrains.pluginverifier.reporting.Reporter

class CollectingReporter<T> : Reporter<T> {

  private val reported = arrayListOf<T>()

  fun getReported(): List<T> = reported

  override fun report(t: T) {
    reported.add(t)
  }

  override fun close() = Unit

}