package com.jetbrains.pluginverifier.reporting

import java.io.Closeable

/**
 * Reporter is responsible for giving the intermediate and end results to the interested clients.
 *
 * @author Sergey Patrikeev
 */
interface Reporter<in T> : Closeable {
  fun report(t: T)
}