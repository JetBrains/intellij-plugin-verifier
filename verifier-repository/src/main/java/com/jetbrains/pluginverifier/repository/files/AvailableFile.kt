package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.FileLock
import java.io.File

data class AvailableFile<out K>(
    val key: K,
    val file: File,
    val size: Long,
    val registeredLocks: Set<FileLock>
)