package com.jetbrains.pluginverifier.repository.cleanup

import org.apache.commons.io.FileUtils

data class DiskSpaceSetting(
    val maxSpaceUsage: Long,
    val lowSpaceThreshold: Long = FileUtils.ONE_GB,
    val enoughSpaceThreshold: Long = lowSpaceThreshold * 3
)