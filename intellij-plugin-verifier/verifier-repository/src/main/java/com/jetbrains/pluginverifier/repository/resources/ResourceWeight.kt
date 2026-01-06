/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.resources

/**
 * Resource weight is a cost of storing the resources
 * in the [repository][ResourceRepository].
 *
 * It is used to determine a set of resources for deletion on the
 * [cleanup][EvictionPolicy] procedure.
 */
interface ResourceWeight<W : ResourceWeight<W>> : Comparable<W> {
  operator fun plus(other: W): W

  operator fun minus(other: W): W
}