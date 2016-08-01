package org.jetbrains.plugins.verifier.service.util

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File

/**
 * @author Sergey Patrikeev
 */
private val LOG = LoggerFactory.getLogger("GlobalLogger")

fun <T> T?.notNullize(default: T) = if (this == null) default else this

fun Closeable.closeLogged() {
  try {
    this.close()
  } catch(e: Exception) {
    LOG.error("Unable to close $this", e)
  }
}

fun File.deleteLogged(): Boolean {
  try {
    FileUtils.forceDelete(this)
    return true
  } catch(e: Exception) {
    LOG.error("Unable to delete $this", e)
    return false
  }
}

fun String.pluralize(times: Int): String {
  if (times < 0) throw IllegalArgumentException("Negative value")
  if (times == 0) return ""
  if (times == 1) return this else return this + "s"
}