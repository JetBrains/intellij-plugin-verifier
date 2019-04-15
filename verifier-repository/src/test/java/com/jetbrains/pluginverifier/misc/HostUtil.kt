package com.jetbrains.pluginverifier.misc

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import java.net.URL

fun checkHostIsAvailable(url: URL): Boolean = try {
  val connection = url.openConnection()
  connection.connect()
  connection.getInputStream().close()
  true
} catch (e: Exception) {
  e.rethrowIfInterrupted()
  System.err.println("URL is not available at the moment: ${url.toExternalForm()}")
  e.printStackTrace()
  false
}
