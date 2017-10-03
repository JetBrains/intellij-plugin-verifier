package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.base.logging.Logger
import com.jetbrains.plugin.structure.base.logging.LoggerFactory
import java.io.Closeable

/**
 * @author Sergey Patrikeev
 */
private val LOG: Logger = LoggerFactory.createDefaultLogger("LanguageUtils")

fun <T : Closeable?> T.closeLogged() {
  try {
    this?.close()
  } catch (e: Exception) {
    LOG.error("Unable to close $this", e)
  }
}

fun <T> Iterator<T>.toList() = asSequence().toList()

fun <T> Iterator<T>.toSet() = asSequence().toSet()