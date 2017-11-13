package com.jetbrains.pluginverifier.repository.validation

import java.io.File

interface FileValidator {
  fun validate(file: File): ValidationResult

  fun isValid(file: File): Boolean = validate(file) == ValidationResult.ValidFile

  sealed class ValidationResult {
    object ValidFile : ValidationResult()

    data class InvalidFile(val reason: String) : ValidationResult()
  }
}