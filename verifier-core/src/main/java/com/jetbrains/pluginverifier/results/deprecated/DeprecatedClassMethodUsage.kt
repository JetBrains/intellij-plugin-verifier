package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.NO_HOST
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation
import com.jetbrains.pluginverifier.results.presentation.methodOrConstructorWord
import java.util.*

class DeprecatedClassMethodUsage(
    override val deprecatedElement: ClassLocation,
    override val usageLocation: Location,
    val method: MethodLocation
) : DeprecatedApiUsage() {
  override val shortDescription = method.methodOrConstructorWord.capitalize() + " of a deprecated class is used ${method.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, SIMPLE_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE)}"

  override val fullDescription = method.methodOrConstructorWord.capitalize() + " ${method.formatMethodLocation(NO_HOST, SIMPLE_PARAM_CLASS_NAME, SIMPLE_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE)} of the deprecated class ${deprecatedElement.formatClassLocation(FULL_NAME, WITH_GENERICS)} is used in ${usageLocation.formatDeprecatedUsageLocation()}"

  override fun equals(other: Any?) = other is DeprecatedClassMethodUsage
      && deprecatedElement == other.deprecatedElement
      && usageLocation == other.usageLocation
      && method == other.method

  override fun hashCode() = Objects.hash(deprecatedElement, usageLocation, method)

}