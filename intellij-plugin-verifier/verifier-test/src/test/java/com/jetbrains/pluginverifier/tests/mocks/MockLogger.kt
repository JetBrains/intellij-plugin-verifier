package com.jetbrains.pluginverifier.tests.mocks

import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.AbstractLogger

/**
 * Mock SLF4J logger that aggregates logging events into in-memory list of logging events.
 *
 * This is used to assert logging events in unit tests.
 */
class MockLogger : AbstractLogger() {
  private val _loggingEvents = mutableListOf<LoggingEvent>()

  val loggingEvents: List<LoggingEvent>
    get() = _loggingEvents

  override fun handleNormalizedLoggingCall(level: Level?, marker: Marker?, messagePattern: String?, arguments: Array<out Any>?, throwable: Throwable?) {
    _loggingEvents.add(LoggingEvent(level, marker, messagePattern, arguments, throwable))
  }

  override fun isTraceEnabled() = true

  override fun isTraceEnabled(marker: Marker?) = true

  override fun isDebugEnabled() = true

  override fun isDebugEnabled(marker: Marker?) = true

  override fun isInfoEnabled() = true

  override fun isInfoEnabled(marker: Marker?) = true

  override fun isWarnEnabled() = true

  override fun isWarnEnabled(marker: Marker?) = true

  override fun isErrorEnabled() = true

  override fun isErrorEnabled(marker: Marker?) = true

  override fun getFullyQualifiedCallerName() = "com.jetbrains.mock"

  data class LoggingEvent(val level: Level?, val marker: Marker?, val messagePattern: String?, val arguments: Array<out Any>?, val throwable: Throwable?)
}