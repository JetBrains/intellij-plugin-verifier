/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.resources

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import org.slf4j.Logger
import java.util.*

/**
 * Data structure that maintains a set of registered resources and their total weights.
 *
 * It is initialized with initial weight of [totalWeight],
 * typically equal zero in the units of chosen weights domain, the [weigher] used to assign
 * weights of the resources in a controlled way and the [disposer] used to deallocate
 * the resources being removed.
 */
internal class RepositoryResourcesRegistrar<R, K, W : ResourceWeight<W>>(
  initWeight: W,
  private val weigher: (R) -> W,
  private val disposer: (R) -> Unit,
  private val logger: Logger
) {

  private var _totalWeight: W = initWeight

  private val _resources = hashMapOf<K, ResourceInfo<R, W>>()

  val totalWeight: W
    get() = _totalWeight

  val resources: Map<K, ResourceInfo<R, W>>
    get() = _resources.toMap()

  val entries: Set<Map.Entry<K, ResourceInfo<R, W>>> get() = Collections.unmodifiableSet(_resources.entries)

  /**
   * Associates the [resource] with [key].
   *
   * It disposes the [resource] if the [key] is
   * already associated, or if an exception occurs.
   *
   * Returns `true` if the resource was added,
   * and `false` if the [key] is occupied.
   */
  fun addResource(key: K, resource: R): Boolean {
    try {
      if (key in _resources) {
        logger.debugMaybe { "add($key): the $resource is already available. Disposing the duplicate resource." }
        safeDispose(key, resource)
        return false
      }

      val resourceWeight = weigher(resource)
      _totalWeight += resourceWeight
      logger.debugMaybe { "add($key): adding the $resource of weight $resourceWeight. Total weight: $_totalWeight" }
      _resources[key] = ResourceInfo(resource, resourceWeight)
      return true
    } catch (e: Throwable) {
      safeDispose(key, resource)
      throw e
    }
  }

  fun getAllKeys() = _resources.keys.toSet()

  fun has(key: K) = key in _resources

  fun get(key: K) = _resources[key]

  fun removeResource(key: K) {
    check(key in _resources)
    val resourceInfo = _resources[key]!!
    val resource = resourceInfo.resource
    val weight = resourceInfo.weight
    _totalWeight -= weight
    logger.debugMaybe { "remove($key): removing the $resource of weight $weight. Total weight: $_totalWeight" }
    _resources.remove(key)
    safeDispose(key, resource)
  }

  private fun safeDispose(key: K, resource: R) {
    try {
      logger.debugMaybe { "dispose($key)" }
      disposer(resource)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      logger.error("unable to dispose the resource $resource", e)
    }
  }
}