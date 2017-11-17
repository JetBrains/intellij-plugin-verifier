package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.UsageStatistic
import java.io.File

data class AvailableFile<out K>(
    val key: K,
    val file: File,
    val size: SpaceAmount,
    val usageStatistic: UsageStatistic,
    val isLocked: Boolean
)