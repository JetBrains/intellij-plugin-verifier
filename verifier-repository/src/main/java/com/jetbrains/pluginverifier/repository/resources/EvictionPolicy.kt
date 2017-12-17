package com.jetbrains.pluginverifier.repository.resources

/**
 * Eviction policy determines when the cleanup procedure must be carried out
 * and which resources should be removed.
 */
interface EvictionPolicy<R, K> {

  /**
   * Determines whether it is necessary to carry out the cleanup procedure
   * given the total weight of the resources in the repository is [totalWeight].
   * This method may be called often, so it is expected to return quickly.
   */
  fun isNecessary(totalWeight: ResourceWeight): Boolean

  /**
   * Given the current state of the [repository] [ResourceRepository]
   * determines which resources must be [removed] [ResourceRepository.remove].
   */
  fun selectResourcesForDeletion(evictionInfo: EvictionInfo<R, K>): List<AvailableResource<R, K>>
}