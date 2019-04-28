package com.jetbrains.pluginverifier.reporting

import java.io.Closeable

/**
 * Reporter is responsible for providing intermediate and
 * end results of a process to interested clients.
 */
interface Reporter<in T> : Closeable {
  fun report(t: T)
}