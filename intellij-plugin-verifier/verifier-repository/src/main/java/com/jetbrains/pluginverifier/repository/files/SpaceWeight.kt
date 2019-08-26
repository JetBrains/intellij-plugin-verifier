package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.resources.ResourceWeight

/**
 * Resource weight equal to the disk space occupied by the
 * file in the [repository] [com.jetbrains.pluginverifier.repository.files.FileRepository].
 */
data class SpaceWeight(val spaceAmount: SpaceAmount) : ResourceWeight<SpaceWeight> {

  override fun plus(other: SpaceWeight) =
      SpaceWeight(spaceAmount + other.spaceAmount)

  override fun minus(other: SpaceWeight) =
      SpaceWeight(spaceAmount - other.spaceAmount)

  override fun compareTo(other: SpaceWeight) =
      spaceAmount.compareTo(other.spaceAmount)

  override fun toString() = spaceAmount.toString()

}