package com.jetbrains.plugin.structure.classes.resolvers

import com.google.common.base.Suppliers
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

object JarFileSystemDebug {

  private val LOG = LoggerFactory.getLogger("JarFileSystems")

  private val debugLogIsActive = Suppliers.memoizeWithExpiration<Boolean>(
    { System.getProperty("intellij.structure.jar.file.systems.debug.level", "false") != "false" },
    5, TimeUnit.SECONDS
  )

  fun debugMessage(message: String) {
    if (debugLogIsActive.get()) {
      // Use "info" level instead of "debug" to make sure the log is displayed
      // even if slf4j is accidentally configured to ignore it.
      LOG.info(message)
    }
  }

}