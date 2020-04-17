/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.resources

/**
 * Aggregates information on the current state of
 * the [repository] [ResourceRepository].
 * This information is used by the [EvictionPolicy] to determine a set
 * of resources to be removed on the cleanup procedure.
 */
data class EvictionInfo<out R, out K, W : ResourceWeight<W>>(
  /**
   * The total weight of the resources at the moment
   */
  val totalWeight: W,

  /**
   * The currently available resources
   */
  val availableResources: List<AvailableResource<R, K, W>>
)