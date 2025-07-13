/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.zip

import java.io.File

sealed class ZipArchiveException(zipFile: File, reason: String, cause: Throwable) : RuntimeException("$reason ${zipFile.type} archive [$zipFile]: ${cause.message}", cause)
class MalformedZipArchiveException(zipFile: File, cause: Throwable) : ZipArchiveException(zipFile, "Malformed", cause)
class ZipArchiveIOException(zipFile: File, cause: Throwable) : ZipArchiveException(zipFile, "Unreadable or I/O error in", cause)

private val File.type: String
  get() = when (extension.toLowerCase()) {
    "zip" -> "ZIP"
    "jar" -> "JAR"
    else -> extension.toUpperCase() + " Archive"
  }
