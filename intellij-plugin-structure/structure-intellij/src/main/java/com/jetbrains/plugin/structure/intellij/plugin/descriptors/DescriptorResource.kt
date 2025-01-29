/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.descriptors

import java.io.InputStream
import java.net.URI
import java.nio.file.Path

/**
 * Represents a plugin descriptor as a [InputStream]-based resource identified by a specific [URI] and an optional
 * parent that contains this plugin descriptor.
 *
 * @param inputStream stream of bytes that represents XML data of this descriptor
 * @param uri an identifier of this byte stream, usually as a filesystem path in the `file` or `jar` scheme.
 * @param parentDescriptorUri parent descriptor. A descriptor XML might occur within another descriptor as an inline XML CDATA.
 * Then, the parent descriptor URI points to the filesystem path of that containing descriptor.
 */
data class DescriptorResource(val inputStream: InputStream, val uri: URI, val parentDescriptorUri: URI? = null) {
  val fileName: String = extractFileName(uri)

  val filePath: Path = Path.of(fileName)

  val artifactFileName: String = extractArtifactFileName(uri)
}

private fun extractFileName(uri: URI): String = with(uri.toString()) {
  if (startsWith("jar:")) {
    substringAfterLast("!/").substringAfterLast("/")
  } else {
    substringAfterLast("/")
  }
}

private fun extractArtifactFileName(uri: URI): String = with(uri.toString()) {
  if (startsWith("jar:file")) {
    uri.schemeSpecificPart.substringAfter(":").substringBeforeLast("!").removePrefix("//")
  } else {
    this
  }
}

