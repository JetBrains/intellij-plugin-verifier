/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.resources

/**
 * Resource repository is a data structure that maintains
 * a set of [resources][R] identified by unique [keys][K].
 *
 * The essential difference from the standard cache, such as
 * one provided by guava cache or [caffeine cache][com.github.benmanes.caffeine.cache.Cache],
 * is that for all the keys being [fetched][get] the [resource lock][ResourceLock]
 * is registered. This resource lock, until being [released][ResourceLock.release],
 * protects the key from being [removed][remove] from cache by other threads.
 *
 * This useful protection property is intended for the cases in which
 * several concurrent threads access the same shareable resource, such as
 * a file. It is necessary to ensure that no thread will remove the file
 * while it is used by any other thread. Once the file becomes unused, it can be
 * safely removed.
 *
 * The main [implementation][ResourceRepositoryImpl], in addition to those properties,
 * provides means to limit the total [weight][W] of the resources being kept.
 * In the above example with files, this can be used to set up the maximum disk space
 * used by all the available files. Once the space exceeds the limit, the unused files
 * become evicted. The files for eviction are chosen using a configurable [policy][EvictionPolicy].
 */
interface ResourceRepository<R : Any, K : Any, W : ResourceWeight<W>> {

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
   * has been provided, a [resource lock][ResourceLock] is registered
   * for the resource, so it will be protected against
   * deletions by other threads.
   */
  fun get(key: K): ResourceRepositoryResult<R, W>

  /**
   * Adds the [resource] by specified [key] to this repository
   * if the key is not available. Otherwise, it has no effect.
   *
   * @return `true` if the [resource] has been added,
   * `false` if the resource by the specified [key] is already present.
   */
  fun add(key: K, resource: R): Boolean

  /**
   * Removes the resource by specified [key] from this repository
   * if the key is [available][has] and there are no registered locks for
   * the resource by this key.
   *
   * If the resource is not available in the repository, the [remove] produces no effect
   * and `false` is returned.
   *
   * If the resource is locked or is being fetched by another thread
   * at the time of [remove] invocation, deletion of the resource
   * from the repository is postponed until all locks of the
   * resource are released. `false` is returned in this case.
   */
  fun remove(key: K): Boolean

  /**
   * Removes all the [available][has] keys from this repository.
   * This method behaves as if by invoking of the following code:
   * ```
   * with(resourceRepository) {
   *   lockAndExecute {
   *     getAllExistingKeys().forEach { remove(it) }
   *   }
   * }
   * ```
   *
   * Thus, each non-locked resource is [removed][remove] immediately,
   * while the locked keys are scheduled for removing once
   * their holders [release][ResourceLock.release] the resource locks.
   */
  fun removeAll()

  /**
   * Returns `true` if the resource by specified [key] is available in
   * the repository, `false` otherwise.
   */
  fun has(key: K): Boolean

  /**
   * Returns `true` if the resource by specified [key] is available in
   * the repository and is locked, or it is being provided,
   * at the moment of invocation of this method.
   * Returns `false` otherwise.
   */
  fun isLockedOrBeingProvided(key: K): Boolean

  /**
   * Returns all keys [available][has] in the repository at the moment.
   *
   * The returned value is a copy of the internal set.
   */
  fun getAllExistingKeys(): Set<K>

  /**
   * Returns all available resources in the repository at the moment.
   *
   * The returned value is a copy of the internal set.
   */
  fun getAvailableResources(): List<AvailableResource<R, K, W>>

  /**
   * Perform the cleanup procedure that frees the resources
   * selected by the implementation of this interface.
   */
  fun cleanup()

}