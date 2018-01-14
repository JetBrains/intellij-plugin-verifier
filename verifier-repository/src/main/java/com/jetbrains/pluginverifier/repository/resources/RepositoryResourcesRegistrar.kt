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

  fun addResource(key: K, resource: R): Boolean {
    if (key in resources) {
      logger.debug("add($key): the $resource is already available. Disposing the duplicate resource.")
      safeDispose(key, resource)
      return false
    }

    val resourceWeight = weigher(resource)
    totalWeight += resourceWeight
    logger.debug("add($key): adding the $resource of weight $resourceWeight. Total weight: $totalWeight")
    resources[key] = ResourceInfo(resource, resourceWeight)
    return true
  }

  fun getAllKeys() = resources.keys

  fun has(key: K) = key in resources

  fun get(key: K) = resources[key]

  fun removeResource(key: K) {
    assert(key in resources)
    val resourceInfo = resources[key]!!
    val resource = resourceInfo.resource
    val weight = resourceInfo.weight
    totalWeight -= weight
    logger.debug("remove($key): removing the $resource of weight $weight. Total weight: $totalWeight")
    resources.remove(key)
    safeDispose(key, resource)
  }

  private fun safeDispose(key: K, resource: R) {
    try {
      logger.debug("dispose($key)")
      disposer(resource)
    } catch (e: Exception) {
      logger.error("unable to dispose the resource $resource", e)
    }
  }
}