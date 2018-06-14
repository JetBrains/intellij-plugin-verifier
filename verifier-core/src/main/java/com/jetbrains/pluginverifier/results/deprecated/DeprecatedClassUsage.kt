package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import java.util.*

class DeprecatedClassUsage(
    override val deprecatedElement: ClassLocation,
    override val usageLocation: Location
) : DeprecatedApiUsage() {
  override val shortDescription
    get() = "Deprecated class ${deprecatedElement.formatClassLocation(FULL_NAME, NO_GENERICS)} reference"

  override val fullDescription
    get() = "Deprecated class ${deprecatedElement.formatClassLocation(FULL_NAME, WITH_GENERICS)} is referenced in " + usageLocation.formatDeprecatedUsageLocation()

  override val deprecatedElementType
    get() = DeprecatedElementType.CLASS

  override fun equals(other: Any?) = other is DeprecatedClassUsage
      && deprecatedElement == other.deprecatedElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(deprecatedElement, usageLocation)

}