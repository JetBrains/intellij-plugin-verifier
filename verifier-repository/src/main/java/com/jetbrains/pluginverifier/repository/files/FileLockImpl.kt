package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.downloader.SpaceWeight
import com.jetbrains.pluginverifier.repository.resources.ResourceLock
import java.nio.file.Path

internal class FileLockImpl(private val resourceLock: ResourceLock<Path>) : FileLock(
    resourceLock.lockTime,
    resourceLock.resource,
    (resourceLock.resourceWeight as SpaceWeight).spaceAmount
) {

  override fun release() = resourceLock.release()

  override fun toString() = "FileLock for ${resourceLock.resource}"
}