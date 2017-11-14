package com.jetbrains.pluginverifier.repository.validation

import java.io.File

class PluginFileValidator : FileValidator {
  private companion object {
    val BROKEN_FILE_THRESHOLD_BYTES = 200
  }

  override fun isValid(file: File): Boolean = file.length() > BROKEN_FILE_THRESHOLD_BYTES
}