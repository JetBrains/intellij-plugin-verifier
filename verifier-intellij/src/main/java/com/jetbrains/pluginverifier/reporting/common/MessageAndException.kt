package com.jetbrains.pluginverifier.reporting.common

/**
 * This class allows to [report] [com.jetbrains.pluginverifier.reporting.Reporter.report]
 * the [message] and [exception].
 * It is needed to pass these two values to the method
 * [report] [com.jetbrains.pluginverifier.reporting.Reporter.report]
 * that expects only one value.
 */
data class MessageAndException(val message: String, val exception: Throwable)