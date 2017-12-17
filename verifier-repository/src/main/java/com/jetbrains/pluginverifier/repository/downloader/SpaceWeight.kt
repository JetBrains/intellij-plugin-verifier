package com.jetbrains.pluginverifier.repository.downloader

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.resources.ResourceWeight

/**
 * Resource weight equal to the disk space occupied by the
 * file in the [repository] [com.jetbrains.pluginverifier.repository.files.FileRepository].
 */
data class SpaceWeight(val spaceAmount: SpaceAmount) : ResourceWeight {

  override fun plus(other: ResourceWeight) =
      SpaceWeight(spaceAmount + (other as SpaceWeight).spaceAmount)

  override fun minus(other: ResourceWeight) =
      SpaceWeight(spaceAmount - (other as SpaceWeight).spaceAmount)

  override fun compareTo(other: ResourceWeight) =
      spaceAmount.compareTo((other as SpaceWeight).spaceAmount)

  override fun toString() = spaceAmount.toString()

}