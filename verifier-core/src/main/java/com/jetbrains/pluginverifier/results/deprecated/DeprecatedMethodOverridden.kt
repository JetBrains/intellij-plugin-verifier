package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.NO_PARAMETER_NAMES
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.FULL_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.FULL_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.NO_RETURN_TYPE
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation
import java.util.*

class DeprecatedMethodOverridden(
    override val deprecatedElement: MethodLocation,
    override val usageLocation: MethodLocation,
    override val deprecationInfo: DeprecationInfo
) : DeprecatedApiUsage() {
  override val shortDescription
    get() = "Deprecated method ${deprecatedElement.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, NO_RETURN_TYPE, NO_PARAMETER_NAMES)} is overridden"

  override val fullDescription
    get() = buildString {
      append("Deprecated method ${deprecatedElement.formatMethodLocation(FULL_HOST_NAME, FULL_PARAM_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE)}")
      append(" is overridden in class ${usageLocation.hostClass.formatClassLocation(FULL_NAME, NO_GENERICS)}")
      if (deprecationInfo.untilVersion != null) {
        append(". This method will be removed in " + deprecationInfo.untilVersion)
      }
    }

  override val deprecatedElementType
    get() = DeprecatedElementType.METHOD

  override fun equals(other: Any?) = other is DeprecatedMethodOverridden
      && deprecatedElement == other.deprecatedElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(deprecatedElement, usageLocation)

}