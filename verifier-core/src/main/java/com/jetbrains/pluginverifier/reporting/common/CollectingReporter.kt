package com.jetbrains.pluginverifier.reporting.common

import com.jetbrains.pluginverifier.reporting.Reporter

class CollectingReporter<T> : Reporter<T> {

  private val _reported: MutableList<T> = arrayListOf<T>()

  val allReported: List<T>
    get() = _reported

  override fun report(t: T) {
    _reported += t
  }

  override fun close() = Unit

}