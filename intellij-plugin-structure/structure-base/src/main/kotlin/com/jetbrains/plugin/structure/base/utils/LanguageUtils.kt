package com.jetbrains.plugin.structure.base.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable

private val logger: Logger = LoggerFactory.getLogger("LanguageUtils")

fun <T : Closeable?> T.closeLogged() {
  try {
    this?.close()
  } catch (e: Exception) {
    logger.error("Unable to close $this", e)
  }
}

fun <T> Iterator<T>.toList() = asSequence().toList()

fun <T> Iterator<T>.toSet() = asSequence().toSet()