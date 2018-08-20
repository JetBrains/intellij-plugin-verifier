package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation
import java.util.*

class DeprecatedFieldUsage(
    override val deprecatedElement: FieldLocation,
    override val usageLocation: Location,
    override val deprecationInfo: DeprecationInfo
) : DeprecatedApiUsage() {
  override val shortDescription
    get() = "Deprecated field ${deprecatedElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.NO_TYPE)} access"

  override val fullDescription
    get() = buildString {
      append("Deprecated field ${deprecatedElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.FULL_TYPE)} is")
      append(" accessed in ${usageLocation.formatDeprecatedUsageLocation()}")
      if (deprecationInfo.forRemoval) {
        append(". This field will will be removed in ")
        append(deprecationInfo.untilVersion ?: " a future release")
      }
    }

  override val deprecatedElementType
    get() = DeprecatedElementType.FIELD

  override fun equals(other: Any?) = other is DeprecatedFieldUsage
      && deprecatedElement == other.deprecatedElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(deprecatedElement, usageLocation)

}