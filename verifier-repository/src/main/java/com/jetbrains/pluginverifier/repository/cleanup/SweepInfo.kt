package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.files.AvailableFile

/**
 * Aggregates information on the current state of
 * the [repository] [com.jetbrains.pluginverifier.repository.files.FileRepository].
 * This information is used by the [SweepPolicy] to determine the set
 * of files to be deleted on the cleanup procedure.
 */
data class SweepInfo<K>(
    /**
     * The total amount of disk space used at the moment
     */
    val totalSpaceUsed: SpaceAmount,

    /**
     * All the currently available files
     */
    val availableFiles: List<AvailableFile<K>>
)