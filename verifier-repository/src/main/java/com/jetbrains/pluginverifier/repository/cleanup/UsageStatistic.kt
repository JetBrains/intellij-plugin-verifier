package com.jetbrains.pluginverifier.repository.cleanup

import java.time.Instant

data class UsageStatistic(var lastAccessTime: Instant,
                          var timesAccessed: Long)