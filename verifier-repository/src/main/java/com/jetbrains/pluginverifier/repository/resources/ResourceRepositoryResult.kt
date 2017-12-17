package com.jetbrains.pluginverifier.repository.resources

/**
 * Possible results of [provision] [ResourceRepository.get] of the resource.
 */
sealed class ResourceRepositoryResult<out R> {
  /**
   * The resource is provided and the [resource lock] [lockedResource]
   * is registered for it.
   */
  data class Found<out R>(val lockedResource: ResourceLock<R>) : ResourceRepositoryResult<R>()

  /**
   * The resource is not found due to [reason].
   */
  data class NotFound<R>(val reason: String) : ResourceRepositoryResult<R>()

  /**
   * The resource is failed to be provided due to [reason].
   * The exception thrown on provision is [error].
   */
  data class Failed<R>(val reason: String, val error: Exception) : ResourceRepositoryResult<R>()
}