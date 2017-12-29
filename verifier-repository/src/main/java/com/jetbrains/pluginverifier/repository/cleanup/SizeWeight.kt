package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.misc.pluralizeWithNumber
import com.jetbrains.pluginverifier.repository.resources.ResourceWeight

/**
 * Resource weight equal to the number of resources
 * available in the [repository] [com.jetbrains.pluginverifier.repository.resources.ResourceRepository].
 */
data class SizeWeight(val size: Long) : ResourceWeight {

  override fun plus(other: ResourceWeight) =
      SizeWeight(size + (other as SizeWeight).size)

  override fun minus(other: ResourceWeight) =
      SizeWeight(size - (other as SizeWeight).size)

  override fun compareTo(other: ResourceWeight) =
      size.compareTo((other as SizeWeight).size)

  override fun toString() = "element".pluralizeWithNumber(size)

}