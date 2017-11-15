package com.jetbrains.pluginverifier.repository.cleanup

data class KeyUsageStatistic<K>(val key: K,
                                var lastAccessTime: Long,
                                var timesAccessed: Long)