package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.utils.exists
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
fun Path.toJarFileUri(): URI = if (toUri().scheme == FILE_SCHEMA) {
  toAbsolutePath().run {
    if (exists()) toRealPath() else normalize()
  }.asJarFileUri()
} else {
  toUri()
}

private fun Path.asJarFileUri(): URI = toUri().replaceSchema(JAR_FILE_SCHEMA)

private fun URI.replaceSchema(newSchema: String) = URI(newSchema, userInfo, host, port, path, query, fragment)