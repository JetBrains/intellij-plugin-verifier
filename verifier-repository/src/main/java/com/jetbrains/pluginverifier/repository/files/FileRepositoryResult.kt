package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.FileLock

sealed class FileRepositoryResult {
  data class Found(val lockedFile: FileLock) : FileRepositoryResult()

  data class NotFound(val reason: String) : FileRepositoryResult()

  data class Failed(val reason: String, val error: Exception) : FileRepositoryResult()
}