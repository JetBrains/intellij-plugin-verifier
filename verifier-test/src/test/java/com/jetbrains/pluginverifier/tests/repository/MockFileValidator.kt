package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.validation.FileValidator
import java.io.File

class MockFileValidator : FileValidator {
  override fun isValid(file: File): Boolean = true
}