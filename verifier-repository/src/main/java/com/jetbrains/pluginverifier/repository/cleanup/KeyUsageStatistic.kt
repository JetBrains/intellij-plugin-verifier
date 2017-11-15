package com.jetbrains.pluginverifier.repository.cleanup

import java.time.Instant

data class KeyUsageStatistic<K>(val key: K,
                                var lastAccessTime: Instant,
                                var timesAccessed: Long)