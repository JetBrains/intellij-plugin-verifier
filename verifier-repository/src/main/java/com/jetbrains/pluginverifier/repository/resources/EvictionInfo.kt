package com.jetbrains.pluginverifier.repository.resources

/**
 * Aggregates information on the current state of
 * the [repository] [ResourceRepository].
 * This information is used by the [EvictionPolicy] to determine a set
 * of resources to be removed on the cleanup procedure.
 */
data class EvictionInfo<out R, out K>(
    /**
     * The total weight of the resources at the moment
     */
    val totalWeight: ResourceWeight,

    /**
     * The currently available resources
     */
    val availableResources: List<AvailableResource<R, K>>
)