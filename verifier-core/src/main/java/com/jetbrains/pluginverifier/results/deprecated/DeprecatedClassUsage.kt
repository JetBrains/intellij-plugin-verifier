package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation

data class DeprecatedClassUsage(override val deprecatedElement: ClassLocation,
                                override val usageLocation: Location) : DeprecatedApiUsage() {
  override val shortDescription = "Deprecated class usage ${deprecatedElement.formatClassLocation(FULL_NAME, NO_GENERICS)}"

  override val fullDescription = "Deprecated class ${deprecatedElement.formatClassLocation(FULL_NAME, WITH_GENERICS)} is used in " + usageLocation.formatDeprecatedUsageLocation()
}