/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.resources

import java.time.Instant

internal class ResourceLockImpl<R, K, W : ResourceWeight<W>>(
  lockTime: Instant,
  resourceInfo: ResourceInfo<R, W>,
  val key: K,
  private val lockId: Long,
  private val repository: ResourceRepositoryImpl<R, K, W>
) : ResourceLock<R, W>(lockTime, resourceInfo) {

  override fun release() = repository.releaseLock(this)

  override fun equals(other: Any?) = other is ResourceLockImpl<*, *, *> && lockId == other.lockId && repository === other.repository

  override fun hashCode() = lockId.hashCode()

  override fun toString() = "(key = $key; lockId = $lockId; resource = $resource)"

}