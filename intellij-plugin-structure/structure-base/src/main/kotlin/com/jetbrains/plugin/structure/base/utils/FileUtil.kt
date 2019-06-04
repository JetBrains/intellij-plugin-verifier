package com.jetbrains.plugin.structure.base.utils

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.slf4j.LoggerFactory
import java.io.File

private val LOG = LoggerFactory.getLogger("structure.FileUtil")

fun File.isZip(): Boolean = this.hasExtension("zip")

fun File.isJar(): Boolean = this.hasExtension("jar")

fun File.hasExtension(expected: String) =
    isFile && expected == extension

fun File.forceDeleteIfExists() {
  if (exists()) {
    FileUtils.forceDelete(this)
  }
}

fun File.listRecursivelyAllFilesWithName(name: String): Collection<File> =
    FileUtils.listFiles(this, NameFileFilter(name), TrueFileFilter.TRUE)

fun File.listRecursivelyAllFilesWithExtension(extension: String): Collection<File> =
    FileUtils.listFiles(this, WildcardFileFilter("*.$extension"), TrueFileFilter.TRUE)

fun File.deleteLogged(): Boolean = try {
  forceDeleteIfExists()
  true
} catch (ie: InterruptedException) {
  Thread.currentThread().interrupt()
  LOG.info("Cannot delete file because of interruption:  $this")
  false
} catch (e: Exception) {
  LOG.error("Unable to delete $this", e)
  false
}