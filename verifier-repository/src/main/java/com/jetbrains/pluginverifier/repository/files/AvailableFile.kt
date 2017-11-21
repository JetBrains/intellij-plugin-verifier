package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.UsageStatistic

data class AvailableFile<out K>(
    val key: K,
    val fileInfo: FileInfo,
    val usageStatistic: UsageStatistic,
    val isLocked: Boolean
)