package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation
import com.jetbrains.pluginverifier.results.usage.formatUsageLocation
import java.util.*

class DeprecatedFieldUsage(
    override val apiElement: FieldLocation,
    override val usageLocation: Location,
    deprecationInfo: DeprecationInfo
) : DeprecatedApiUsage(deprecationInfo) {

  override val shortDescription
    get() = "Deprecated field ${apiElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.NO_TYPE)} access"

  override val fullDescription
    get() = buildString {
      append("Deprecated field ${apiElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.FULL_TYPE)} is")
      append(" accessed in ${usageLocation.formatUsageLocation()}")
      if (deprecationInfo.forRemoval) {
        append(". This field will will be removed in ")
        append(deprecationInfo.untilVersion ?: " a future release")
      }
    }

  override fun equals(other: Any?) = other is DeprecatedFieldUsage
      && apiElement == other.apiElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiElement, usageLocation)

  companion object {
    private const val serialVersionUID = 0L
  }
}