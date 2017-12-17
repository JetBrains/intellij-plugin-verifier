package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.resources.ResourceLock
import java.nio.file.Path
import java.time.Instant

internal data class FileLockImpl(private val resourceLock: ResourceLock<Path>) : FileLock {

  override val file: Path = resourceLock.resource

  override val lockTime: Instant = resourceLock.lockTime

  override fun release() = resourceLock.release()
}