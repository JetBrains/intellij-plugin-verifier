package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption.NO_TYPE
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption.SIMPLE_TYPE
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.NO_HOST
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation
import java.util.*

class DeprecatedClassFieldUsage(
    override val deprecatedElement: ClassLocation,
    override val usageLocation: Location,
    val field: FieldLocation
) : DeprecatedApiUsage() {
  override val shortDescription = "Field of a deprecated class is used ${field.formatFieldLocation(FULL_HOST_NAME, NO_TYPE)}"

  override val fullDescription = "Field ${field.formatFieldLocation(NO_HOST, SIMPLE_TYPE)} of the deprecated " +
      "class ${deprecatedElement.formatClassLocation(FULL_NAME, WITH_GENERICS)} is used in ${usageLocation.formatDeprecatedUsageLocation()}"

  override fun equals(other: Any?) = other is DeprecatedClassFieldUsage
      && deprecatedElement == other.deprecatedElement
      && usageLocation == other.usageLocation
      && field == other.field

  override fun hashCode() = Objects.hash(deprecatedElement, usageLocation, field)
}