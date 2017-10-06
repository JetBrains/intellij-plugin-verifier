package com.jetbrains.pluginverifier.reporting

import java.io.Closeable

/**
 * @author Sergey Patrikeev
 */
interface Reporter<in T> : Closeable {
  fun report(t: T)
}