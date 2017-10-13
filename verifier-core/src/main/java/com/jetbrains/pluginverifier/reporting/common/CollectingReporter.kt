package com.jetbrains.pluginverifier.reporting.common

import com.jetbrains.pluginverifier.reporting.Reporter

/**
 * @author Sergey Patrikeev
 */
open class CollectingReporter<T> : Reporter<T> {

  private val reported: MutableList<T> = arrayListOf<T>()

  fun getReported(): List<T> = reported

  override fun report(t: T) {
    reported.add(t)
  }

  override fun close() = Unit

}