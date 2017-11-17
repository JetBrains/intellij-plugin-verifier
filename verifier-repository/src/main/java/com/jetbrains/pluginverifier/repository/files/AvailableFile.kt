package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.UsageStatistic
import java.io.File

data class AvailableFile<K>(
    val key: K,
    val file: File,
    val size: Long,
    val usageStatistic: UsageStatistic
)