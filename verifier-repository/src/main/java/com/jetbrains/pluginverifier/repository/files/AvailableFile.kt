package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.UsageStatistic

/**
 * Descriptor of the file available at the current moment in the [FileRepository].
 *
 * This is used to select the files that should be removed on the
 * [cleanup procedure] [com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy].
 */
data class AvailableFile<out K>(
    /**
     * The key of the file in the [repository] [FileRepository]
     */
    val key: K,
    /**
     * File descriptor
     */
    val fileInfo: FileInfo,
    /**
     * Usage statistics of the file
     */
    val usageStatistic: UsageStatistic,
    /**
     * Indicates whether the file is currently locked in the [FileRepository]
     */
    val isLocked: Boolean
)