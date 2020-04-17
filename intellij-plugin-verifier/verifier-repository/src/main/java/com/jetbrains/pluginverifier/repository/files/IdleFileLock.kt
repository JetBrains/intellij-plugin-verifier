/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import java.nio.file.Path
import java.time.Instant

/**
 * The [file lock] [FileLock] instance that doesn't require
 * the file to be locked in a [repository] [com.jetbrains.pluginverifier.repository.files.FileRepository].
 */
class IdleFileLock(file: Path) : FileLock(Instant.ofEpochMilli(0), file, SpaceAmount.ZERO_SPACE) {
  override fun release() = Unit
}