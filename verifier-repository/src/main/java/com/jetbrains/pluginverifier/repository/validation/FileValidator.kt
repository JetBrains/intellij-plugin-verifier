package com.jetbrains.pluginverifier.repository.validation

import java.io.File

interface FileValidator {

  fun isValid(file: File): Boolean

}