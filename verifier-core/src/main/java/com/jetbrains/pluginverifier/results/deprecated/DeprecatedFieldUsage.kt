package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation

/**
 * @author Sergey Patrikeev
 */
data class DeprecatedFieldUsage(override val deprecatedElement: FieldLocation,
                                override val usageLocation: Location) : DeprecatedApiUsage() {
  override val description: String = "Deprecated field ${deprecatedElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.SIMPLE_TYPE)} is used in ${usageLocation.formatUsageLocation()}"
}