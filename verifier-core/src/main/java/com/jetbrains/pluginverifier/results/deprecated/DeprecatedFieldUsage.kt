package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation
import java.util.*

data class DeprecatedFieldUsage(
    override val deprecatedElement: FieldLocation,
    override val usageLocation: Location
) : DeprecatedApiUsage() {
  override val shortDescription = "Deprecated field ${deprecatedElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.NO_TYPE)} access"

  override val fullDescription = "Deprecated field ${deprecatedElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.FULL_TYPE)} is accessed in ${usageLocation.formatDeprecatedUsageLocation()}"

  override val deprecatedElementType = DeprecatedElementType.FIELD

  override fun equals(other: Any?) = other is DeprecatedFieldUsage
      && deprecatedElement == other.deprecatedElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(deprecatedElement, usageLocation)

}