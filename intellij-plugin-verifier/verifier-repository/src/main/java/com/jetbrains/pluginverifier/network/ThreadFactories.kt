package com.jetbrains.pluginverifier.network

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

fun threadFactory(nameFormat: String? = null, daemon: Boolean = false): ThreadFactory {
  val count = AtomicInteger()
  return ThreadFactory { r ->
    Thread(r).apply {
      nameFormat?.apply { name = nameFormat.format(count.getAndIncrement()) }
      isDaemon = daemon
    }
  }
}
