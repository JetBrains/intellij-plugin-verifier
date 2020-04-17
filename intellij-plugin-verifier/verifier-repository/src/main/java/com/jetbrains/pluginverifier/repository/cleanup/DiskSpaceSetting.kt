/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.cleanup

/**
 * Holds the disk space settings.
 *
 * For instance, a setting
 * DiskSpaceSetting(ONE_GIGO_BYTE, ONE_MEGA_BYTE * 100, ONE_MEGA_BYTE * 200)
 * specifies that a directory must not occupy more than 1 Gb of disk space,
 * a cleanup procedure must be carried out whenever free space amount reaches 100 Mb,
 * and it must free up to 200 Mb of disk space.
 */
data class DiskSpaceSetting(
  /**
   * Maximum usage of the disk space
   */
  val maxSpaceUsage: SpaceAmount,
  /**
   * The minimum value of remaining disk space when the cleanup procedure
   * must be performed
   */
  val lowSpaceThreshold: SpaceAmount = maxSpaceUsage * 0.2,
  /**
   * Specified the minimum amount of remaining disk space after the cleanup procedure
   */
  val minimumFreeSpaceAfterCleanup: SpaceAmount = maxSpaceUsage * 0.4
)