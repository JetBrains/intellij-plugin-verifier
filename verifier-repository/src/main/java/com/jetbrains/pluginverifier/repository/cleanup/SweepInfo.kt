package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.files.AvailableFile

data class SweepInfo<K>(val totalSpaceUsed: Long,
                        val availableFiles: List<AvailableFile<K>>,
                        val usageStatistics: Map<K, KeyUsageStatistic<K>>)