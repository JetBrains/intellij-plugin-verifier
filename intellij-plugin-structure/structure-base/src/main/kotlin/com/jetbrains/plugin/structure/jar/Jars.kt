package com.jetbrains.plugin.structure.jar

import java.net.URI
import java.nio.file.Path

const val JAR_FILE_SCHEMA = "jar:file"
const val FILE_SCHEMA = "file"
const val JAR_SCHEME = "jar"

/**
 * Converts the file-based Path to the `jar:file` schema URI prefix.
 * This path is resolved to an absolute path before converting to the URI.
 * All other paths are retained as-is.
 */
fun Path.toJarFileUri(): URI {
  return if (toUri().scheme == FILE_SCHEMA) {
    val absoluteJarPath = if (isAbsolute) this else toAbsolutePath()
    URI("$JAR_FILE_SCHEMA:$absoluteJarPath")
  } else {
    toUri()
  }
}