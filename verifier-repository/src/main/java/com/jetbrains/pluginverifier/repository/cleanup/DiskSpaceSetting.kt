package com.jetbrains.pluginverifier.repository.cleanup

data class DiskSpaceSetting(
    val maxSpaceUsage: SpaceAmount,
    val lowSpaceThreshold: SpaceAmount = SpaceAmount.ofGigabytes(3)
)