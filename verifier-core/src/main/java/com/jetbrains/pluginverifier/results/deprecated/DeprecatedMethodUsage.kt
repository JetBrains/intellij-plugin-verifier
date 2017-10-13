package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation

/**
 * @author Sergey Patrikeev
 */
data class DeprecatedMethodUsage(override val deprecatedElement: MethodLocation,
                                 override val usageLocation: Location) : DeprecatedApiUsage() {
  override val description: String = "Deprecated method ${deprecatedElement.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, SIMPLE_RETURN_TYPE_CLASS_NAME)} is used in " + usageLocation.formatUsageLocation()
}