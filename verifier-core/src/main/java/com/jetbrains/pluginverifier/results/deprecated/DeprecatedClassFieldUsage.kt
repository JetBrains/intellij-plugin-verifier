package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption.SIMPLE_TYPE
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.NO_HOST
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation

/**
 * @author Sergey Patrikeev
 */
data class DeprecatedClassFieldUsage(override val deprecatedElement: ClassLocation,
                                     override val usageLocation: Location,
                                     val fieldLocation: FieldLocation) : DeprecatedApiUsage() {
  override val description: String = "Field ${fieldLocation.formatFieldLocation(NO_HOST, SIMPLE_TYPE)} of the deprecated " +
      "class ${deprecatedElement.formatClassLocation(FULL_NAME, WITH_GENERICS)} is used in ${usageLocation.formatUsageLocation()}"
}