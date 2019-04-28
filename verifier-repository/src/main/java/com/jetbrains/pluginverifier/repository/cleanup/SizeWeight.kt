package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.plugin.structure.base.utils.pluralizeWithNumber
import com.jetbrains.pluginverifier.repository.resources.ResourceWeight

/**
 * Resource weight equal to the number of available resources.
 */
data class SizeWeight(val size: Long) : ResourceWeight<SizeWeight> {

  override fun plus(other: SizeWeight) =
      SizeWeight(size + other.size)

  override fun minus(other: SizeWeight) =
      SizeWeight(size - other.size)

  override fun compareTo(other: SizeWeight) =
      size.compareTo(other.size)

  override fun toString() = "element".pluralizeWithNumber(size)

}