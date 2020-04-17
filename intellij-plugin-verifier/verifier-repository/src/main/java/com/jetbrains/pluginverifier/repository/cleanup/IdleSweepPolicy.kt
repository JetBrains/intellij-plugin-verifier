/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.files.AvailableFile

/**
 * [SweepPolicy] that doesn't sweep any files.
 */
class IdleSweepPolicy<T> : SweepPolicy<T> {
  override fun isNecessary(totalSpaceUsed: SpaceAmount) = false

  override fun selectFilesForDeletion(sweepInfo: SweepInfo<T>) = emptyList<AvailableFile<T>>()
}