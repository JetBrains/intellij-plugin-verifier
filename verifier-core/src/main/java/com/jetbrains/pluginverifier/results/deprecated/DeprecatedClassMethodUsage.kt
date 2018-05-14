package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.NO_HOST
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.NO_PARAMETER_NAMES
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.FULL_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.NO_RETURN_TYPE
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation
import com.jetbrains.pluginverifier.results.presentation.methodOrConstructorWord
import java.util.*

class DeprecatedClassMethodUsage(
    override val deprecatedElement: ClassLocation,
    override val usageLocation: Location,
    val method: MethodLocation
) : DeprecatedApiUsage() {
  override val shortDescription = method.methodOrConstructorWord.capitalize() + " ${method.formatMethodLocation(NO_HOST, SIMPLE_PARAM_CLASS_NAME, NO_RETURN_TYPE, NO_PARAMETER_NAMES)} of deprecated class ${deprecatedElement.formatClassLocation(FULL_NAME, NO_GENERICS)} invocation"

  override val fullDescription = method.methodOrConstructorWord.capitalize() + " ${method.formatMethodLocation(NO_HOST, SIMPLE_PARAM_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE)} of deprecated class ${deprecatedElement.formatClassLocation(FULL_NAME, WITH_GENERICS)} is invoked in ${usageLocation.formatDeprecatedUsageLocation()}"

  override fun equals(other: Any?) = other is DeprecatedClassMethodUsage
      && deprecatedElement == other.deprecatedElement
      && usageLocation == other.usageLocation
      && method == other.method

  override fun hashCode() = Objects.hash(deprecatedElement, usageLocation, method)

}