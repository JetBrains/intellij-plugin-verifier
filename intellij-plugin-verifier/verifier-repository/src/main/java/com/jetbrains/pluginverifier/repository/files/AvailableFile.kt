/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.UsageStatistic
import com.jetbrains.pluginverifier.repository.resources.AvailableResource
import com.jetbrains.pluginverifier.repository.resources.ResourceInfo
import java.nio.file.Path

/**
 * Descriptor of the file available at the moment in the [FileRepository].
 *
 * This is used to select the files that should be removed on the
 * [cleanup procedure][com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy].
 */
class AvailableFile<out K>(
  key: K,
  resourceInfo: ResourceInfo<Path, SpaceWeight>,
  usageStatistic: UsageStatistic,
  isLocked: Boolean
) : AvailableResource<Path, K, SpaceWeight>(key, resourceInfo, usageStatistic, isLocked) {
  /**
   * File descriptor
   */
  val fileInfo: FileInfo
    get() = FileInfo(resourceInfo.resource, resourceInfo.weight.spaceAmount)

}