package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.FileLock
import java.io.File

internal class FileLockImpl<K>(override val file: File,
                               override val lockTime: Long,
                               val key: K,
                               private val lockId: Long,
                               private val repository: FileRepositoryImpl<K>) : FileLock {

  override fun release() = repository.releaseLock(this)

  override fun equals(other: Any?): Boolean = other is FileLockImpl<*> && repository === other.repository && lockId == other.lockId

  override fun hashCode(): Int = lockId.hashCode()
}