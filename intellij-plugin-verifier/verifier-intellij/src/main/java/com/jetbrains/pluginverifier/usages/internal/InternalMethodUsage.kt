package com.jetbrains.pluginverifier.usages.internal

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
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

class InternalMethodUsage(
    override val apiElement: MethodLocation,
    override val usageLocation: Location
) : InternalApiUsage() {

  override val shortDescription
    get() = "Internal " + apiElement.elementType.presentableName + " '${apiElement.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, NO_RETURN_TYPE, NO_PARAMETER_NAMES)}' invocation"

  override val fullDescription
    get() = buildString {
      append("Internal " + apiElement.elementType.presentableName + " ")
      append("'" + apiElement.formatMethodLocation(FULL_HOST_NAME, FULL_PARAM_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE) + "'")
      append(" is invoked in '" + usageLocation.formatUsageLocation() + "'")
      append(
          ". This " + apiElement.elementType.presentableName + " is marked with '@org.jetbrains.annotations.ApiStatus.Internal' annotation and indicates " +
              "that the method is not supposed to be used in client code."
      )
    }

  override fun equals(other: Any?) = other is InternalMethodUsage
      && apiElement == other.apiElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiElement, usageLocation)
}