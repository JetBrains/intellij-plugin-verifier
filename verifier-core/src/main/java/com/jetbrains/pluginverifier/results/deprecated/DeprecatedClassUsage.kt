package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation

/**
 * @author Sergey Patrikeev
 */
data class DeprecatedClassUsage(override val deprecatedElement: ClassLocation,
                                override val usageLocation: Location) : DeprecatedApiUsage() {
  override val description: String = "Deprecated class ${deprecatedElement.formatClassLocation(FULL_NAME, WITH_GENERICS)} is used in " + usageLocation.formatUsageLocation()
}