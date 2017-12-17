package com.jetbrains.pluginverifier.repository.resources

import java.time.Instant

internal data class ResourceLockImpl<R, K>(override val resource: R,
                                           override val lockTime: Instant,
                                           val key: K,
                                           private val lockId: Long,
                                           private val repository: ResourceRepositoryImpl<R, K>) : ResourceLock<R> {
  override fun release() = repository.releaseLock(this)

  override fun equals(other: Any?): Boolean = other is ResourceLockImpl<*, *>
      && lockId == other.lockId && repository === other.repository

  override fun hashCode() = lockId.hashCode()
}