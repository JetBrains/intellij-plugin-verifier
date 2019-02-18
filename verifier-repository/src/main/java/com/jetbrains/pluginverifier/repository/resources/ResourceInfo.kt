package com.jetbrains.pluginverifier.repository.resources

/**
 * Descriptor of the resource in the [repository] [ResourceRepository]
 * containing the reference to the [resource] and the cached resource's [weight].
 */
open class ResourceInfo<out R, W : ResourceWeight<W>>(val resource: R, val weight: W)