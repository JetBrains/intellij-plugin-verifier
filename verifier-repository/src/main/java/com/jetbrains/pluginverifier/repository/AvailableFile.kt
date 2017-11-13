package com.jetbrains.pluginverifier.repository

import java.io.File

data class AvailableFile(
    val file: File,
    val size: Long,
    val locks: Set<FileLock>
)