package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.UsageStatistic
import com.jetbrains.pluginverifier.repository.resources.AvailableResource
import java.nio.file.Path

/**
 * Descriptor of the file available at the moment in the [FileRepository].
 *
 * This is used to select the files that should be removed on the
 * [cleanup procedure] [com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy].
 */
data class AvailableFile<out K>(val availableResource: AvailableResource<Path, K>) {

  /**
   * The key of the file in the [repository] [FileRepository]
   */
  val key: K
    get() = availableResource.key

  /**
   * File descriptor
   */
  val fileInfo: FileInfo
    get() = FileInfo(availableResource.resourceInfo)

  /**
   * Usage statistics of the file
   */
  val usageStatistic: UsageStatistic
    get() = availableResource.usageStatistic

  /**
   * Indicates whether the file is currently locked in the [FileRepository]
   */
  val isLocked: Boolean
    get() = availableResource.isLocked
}