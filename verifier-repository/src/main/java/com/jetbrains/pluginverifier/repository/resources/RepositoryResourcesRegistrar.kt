package com.jetbrains.pluginverifier.repository.resources

import org.slf4j.Logger

/**
 * Data structure that maintains a set of registered resources and their total weights.
 *
 * It is initialized with [initial] [totalWeight] total weight of the resources, typically
 * equal zero in the units of chosen weights domain, the [weigher] used to assign
 * weights of the resources in a controlled way and the [disposer] used to deallocate
 * the resources being removed.
 */
internal class RepositoryResourcesRegistrar<R, K>(var totalWeight: ResourceWeight,
                                                  private val weigher: (R) -> ResourceWeight,
                                                  private val disposer: (R) -> Unit,
                                                  private val logger: Logger) {

  val resources: MutableMap<K, ResourceInfo<R>> = hashMapOf()

  fun addResource(key: K, resource: R) {
    assert(key !in resources)
    val resourceWeight = weigher(resource)
    logger.debug("Adding resource by $key of weight $resourceWeight: $resource")
    totalWeight += resourceWeight
    resources[key] = ResourceInfo(resource, resourceWeight)
  }

  fun getAllKeys() = resources.keys

  fun has(key: K) = key in resources

  fun get(key: K) = resources[key]

  fun deleteResource(key: K) {
    assert(key in resources)
    val resourceInfo = resources[key]!!
    val resource = resourceInfo.resource
    val weight = resourceInfo.weight
    logger.debug("Deleting resource by $key of weight $weight: $resource")
    totalWeight -= weight
    resources.remove(key)
    try {
      disposer(resource)
    } catch (e: Exception) {
      logger.error("Unable to dispose the resource $resource", e)
    }
  }
}