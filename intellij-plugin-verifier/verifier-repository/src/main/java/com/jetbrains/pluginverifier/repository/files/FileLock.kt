/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.resources.ResourceLock
import java.nio.file.Path
import java.time.Instant

/**
 * File lock is used to indicate that the [file]
 * is locked in the [file repository] [FileRepository],
 * thus it cannot be removed until the lock is released.
 * by the lock owner.
 */
abstract class FileLock(
  lockTime: Instant,
  val file: Path,
  fileSize: SpaceAmount
) : ResourceLock<Path, SpaceWeight>(lockTime, FileInfo(file, fileSize))