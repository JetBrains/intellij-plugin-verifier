package com.jetbrains.pluginverifier.repository.validation

import java.io.File

class PluginFileValidator : FileValidator {

  private companion object {
    val BROKEN_FILE_THRESHOLD_BYTES = 200
  }

  override fun validate(file: File): FileValidator.ValidationResult = if (file.length() < BROKEN_FILE_THRESHOLD_BYTES) {
    FileValidator.ValidationResult.InvalidFile("Corrupted plugin file")
  } else {
    FileValidator.ValidationResult.ValidFile
  }
}