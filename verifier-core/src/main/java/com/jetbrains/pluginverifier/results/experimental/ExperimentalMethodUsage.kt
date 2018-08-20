package com.jetbrains.pluginverifier.results.experimental

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
import com.jetbrains.pluginverifier.results.usage.formatUsageLocation
import java.util.*

class ExperimentalMethodUsage(
    override val apiElement: MethodLocation,
    override val usageLocation: Location
) : ExperimentalApiUsage() {

  override val shortDescription
    get() = "Experimental API " + apiElement.elementType.presentableName + " ${apiElement.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, NO_RETURN_TYPE, NO_PARAMETER_NAMES)} invocation"

  override val fullDescription
    get() = buildString {
      append("Experimental API " + apiElement.elementType.presentableName + " ")
      append(apiElement.formatMethodLocation(FULL_HOST_NAME, FULL_PARAM_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE))
      append(" is invoked in " + usageLocation.formatUsageLocation())
      append(". This " + apiElement.elementType.presentableName + " can be changed in a future release leading to incompatibilities")
    }

  override fun equals(other: Any?) = other is ExperimentalMethodUsage
      && apiElement == other.apiElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiElement, usageLocation)

  companion object {
    private const val serialVersionUID = 0L
  }
}