package com.jetbrains.pluginverifier.repository.resources

/**
 * Descriptor of the resource in the [repository] [ResourceRepository]
 * containing the reference to the [resource] and the cached resource's [weight].
 */
data class ResourceInfo<out R>(val resource: R, val weight: ResourceWeight)