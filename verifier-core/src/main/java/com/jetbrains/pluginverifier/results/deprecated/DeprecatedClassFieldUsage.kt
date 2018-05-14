package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption.NO_TYPE
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.NO_HOST
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation
import java.util.*

class DeprecatedClassFieldUsage(
    override val deprecatedElement: ClassLocation,
    override val usageLocation: Location,
    val field: FieldLocation
) : DeprecatedApiUsage() {
  override val shortDescription = "Field '${field.fieldName}' of deprecated class ${deprecatedElement.formatClassLocation(FULL_NAME, NO_GENERICS)} is accessed"

  override val fullDescription = "Field ${field.formatFieldLocation(NO_HOST, NO_TYPE)} of deprecated " +
      "class ${deprecatedElement.formatClassLocation(FULL_NAME, WITH_GENERICS)} is accessed in ${usageLocation.formatDeprecatedUsageLocation()}"

  override fun equals(other: Any?) = other is DeprecatedClassFieldUsage
      && deprecatedElement == other.deprecatedElement
      && usageLocation == other.usageLocation
      && field == other.field

  override fun hashCode() = Objects.hash(deprecatedElement, usageLocation, field)
}