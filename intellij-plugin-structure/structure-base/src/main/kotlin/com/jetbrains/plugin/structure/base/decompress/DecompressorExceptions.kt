/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.decompress

import java.io.IOException

abstract class DecompressorException : IOException {
  constructor(message: String) : super(message)

  override val message: String
    get() = super.message ?: ""
}

class EntryNameTooLongException(message: String) : DecompressorException(message) {
  companion object {
    fun ofEntry(name: String) = EntryNameTooLongException("Entry name is too long: $name")
  }
}

class InvalidRelativeEntryNameException(message: String) : DecompressorException(message) {
  companion object {
    fun ofEntry(name: String) = InvalidRelativeEntryNameException("Invalid relative entry name: path traversal outside root of archive in [$name]")
  }
}

class EmptyEntryNameException(message: String) : DecompressorException(message) {
  companion object {
    fun ofEntry(name: String) = EmptyEntryNameException("Resolved entry name cannot be empty: [$name]")
  }
}

class DecompressorSizeLimitExceededException(val sizeLimit: Long) : DecompressorException("Decompressor size limit of $sizeLimit bytes exceeded")