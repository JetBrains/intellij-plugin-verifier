package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.NO_PARAMETER_NAMES
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.FULL_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.FULL_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.NO_RETURN_TYPE
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation
import com.jetbrains.pluginverifier.results.presentation.isConstructor
import com.jetbrains.pluginverifier.results.presentation.methodOrConstructorWord
import java.util.*

data class DeprecatedMethodUsage(
    override val deprecatedElement: MethodLocation,
    override val usageLocation: Location
) : DeprecatedApiUsage() {
  override val shortDescription = "Deprecated " + deprecatedElement.methodOrConstructorWord + " ${deprecatedElement.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, NO_RETURN_TYPE, NO_PARAMETER_NAMES)} invocation"

  override val fullDescription = "Deprecated " + deprecatedElement.methodOrConstructorWord + " ${deprecatedElement.formatMethodLocation(FULL_HOST_NAME, FULL_PARAM_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE)} is invoked in " + usageLocation.formatDeprecatedUsageLocation()

  override val deprecatedElementType: DeprecatedElementType
    get() = if (deprecatedElement.isConstructor) {
      DeprecatedElementType.CONSTRUCTOR
    } else {
      DeprecatedElementType.METHOD
    }

  override fun equals(other: Any?) = other is DeprecatedMethodUsage
      && deprecatedElement == other.deprecatedElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(deprecatedElement, usageLocation)

}