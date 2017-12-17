package com.jetbrains.pluginverifier.repository.resources

import com.jetbrains.pluginverifier.repository.cleanup.UsageStatistic

/**
 * Descriptor of the resource available at the moment in the [ResourceRepository].
 *
 * This is used to select the resources that should be removed on the
 * [cleanup procedure] [EvictionPolicy].
 */
data class AvailableResource<out R, out K>(
    /**
     * The key of the resource in the [repository] [ResourceRepository]
     */
    val key: K,
    /**
     * Resource descriptor
     */
    val resourceInfo: ResourceInfo<R>,
    /**
     * Usage statistics of the resource
     */
    val usageStatistic: UsageStatistic,
    /**
     * Indicates whether the resource is currently locked in the [ResourceRepository]
     */
    val isLocked: Boolean
)