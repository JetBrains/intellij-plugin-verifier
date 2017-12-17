package com.jetbrains.pluginverifier.repository.resources

/**
 * Resource repository is a data structure that maintains
 * a set of _resources_ identified by unique [keys].
 */
interface ResourceRepository<R, K> {

  /**
   * Provides a resource by [key].
   *
   * If the resource is not available in the repository,
   * it is fetched in a manner specified by the implementation.
   *
   * There are several possible outcomes of this method invocation,
   * all represented by instances of the [ResourceRepositoryResult].
   *
   * In case the resource is available in the repository or
   * has been provided, a [resource lock] [ResourceLock] is registered
   * for the resource, so it will be protected against
   * deletions by other threads.
   */
  fun get(key: K): ResourceRepositoryResult<R>

  /**
   * Adds the [resource] by specified [key] to this repository
   * if the key is not available. Otherwise it has no effect.
   *
   * @return `true` if the [resource] has been added,
   * `false` if the resource by the specified [key] is already present.
   */
  fun add(key: K, resource: R): Boolean

  /**
   * Removes the resource by specified [key] from this repository
   * if the key is [available] [has] and there are no registered locks for
   * the resource by this key.
   *
   * If the resource is not available in the repository, the [remove] produces no effect
   * and `false` is returned.
   *
   * If the resource is locked at the time of [remove] invocation, deletion of the resource
   * from the repository is postponed until all locks of the resource are released.
   * The `false` is returned in this case.
   */
  fun remove(key: K): Boolean

  /**
   * Returns `true` if the resource by specified key is available in
   * the repository, `false` otherwise.
   */
  fun has(key: K): Boolean

  /**
   * Returns all keys available in the repository at the moment.
   */
  fun getAllExistingKeys(): Set<K>

  /**
   * Locks the repository for the time of [block] invocation
   * so that the repository's state cannot be changed by other threads.
   *
   * This may be useful in a concurrent environment when
   * it is necessary to perform the operations atomically:
   * for example, a thread could invoke [getAllExistingKeys]
   * and then [get] several resources.
   */
  fun <R> lockAndExecute(block: () -> R): R

  /**
   * Perform the cleanup procedure that frees the resources
   * selected by the implementation of this interface.
   */
  fun cleanup()

}