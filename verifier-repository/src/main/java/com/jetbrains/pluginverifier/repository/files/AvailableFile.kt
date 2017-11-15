package com.jetbrains.pluginverifier.repository.files

import java.io.File

data class AvailableFile<out K>(
    val key: K,
    val file: File,
    val size: Long
)