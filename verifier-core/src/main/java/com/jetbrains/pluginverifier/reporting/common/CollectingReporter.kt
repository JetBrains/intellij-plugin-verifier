package com.jetbrains.pluginverifier.reporting.common

import com.jetbrains.pluginverifier.reporting.Reporter

class CollectingReporter<T> : Reporter<T> {

  private val _reported: MutableList<T> = arrayListOf()

  val allReported: List<T>
    get() = _reported

  override fun report(t: T) {
    if (t != null) {
      _reported += t
    }
  }

  override fun close() = Unit

}