/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.resources

/**
 * Eviction policy determines when the cleanup procedure must be carried out
 * and which resources should be removed.
 */
interface EvictionPolicy<R, K, W : ResourceWeight<W>> {

  /**
   * Determines whether it is necessary to carry out the cleanup procedure
   * given the total weight of the resources in the repository is [totalWeight].
   * This method may be called often, so it is expected to return quickly.
   */
  fun isNecessary(totalWeight: W): Boolean

  /**
   * Given the current state of the [repository] [ResourceRepository]
   * determines which resources must be [removed] [ResourceRepository.remove].
   */
  fun selectResourcesForEviction(evictionInfo: EvictionInfo<R, K, W>): List<AvailableResource<R, K, W>>
}