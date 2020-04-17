/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.resources.ResourceLock
import java.nio.file.Path

internal class FileLockImpl(private val resourceLock: ResourceLock<Path, SpaceWeight>) : FileLock(
  resourceLock.lockTime,
  resourceLock.resource,
  resourceLock.resourceWeight.spaceAmount
) {

  override fun release() = resourceLock.release()

  override fun toString() = "FileLock for ${resourceLock.resource}"
}