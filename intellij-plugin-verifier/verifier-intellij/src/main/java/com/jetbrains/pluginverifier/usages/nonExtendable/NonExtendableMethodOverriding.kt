package com.jetbrains.pluginverifier.usages.nonExtendable

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.location.toReference
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.NO_PARAMETER_NAMES
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.FULL_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.FULL_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.NO_RETURN_TYPE
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

class NonExtendableMethodOverriding(
    override val apiElement: MethodLocation,
    override val usageLocation: Location
) : NonExtendableApiUsage() {

  override val apiReference
    get() = apiElement.toReference()

  override val shortDescription
    get() = "Non-extendable method ${apiElement.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, NO_RETURN_TYPE, NO_PARAMETER_NAMES)} is overridden"

  override val fullDescription
    get() = buildString {
      append("Non-extendable method ")
      append(apiElement.formatMethodLocation(FULL_HOST_NAME, FULL_PARAM_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE))
      append(" is overridden by " + usageLocation.formatUsageLocation() + ". ")
      append(
          "This method is marked with @org.jetbrains.annotations.ApiStatus.NonExtendable annotation, which indicates " +
              "that the method is not supposed to be overridden by client code. " +
              "See documentation of the @ApiStatus.NonExtendable for more info."
      )
    }

  override fun equals(other: Any?) = other is NonExtendableMethodOverriding
      && apiElement == other.apiElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiElement, usageLocation)
}