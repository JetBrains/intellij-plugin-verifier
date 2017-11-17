package com.jetbrains.pluginverifier.repository.cleanup

import org.apache.commons.io.FileUtils

//todo: create a class for space measurement
data class DiskSpaceSetting(
    val maxSpaceUsage: Long,
    val lowSpaceThreshold: Long = FileUtils.ONE_GB
)