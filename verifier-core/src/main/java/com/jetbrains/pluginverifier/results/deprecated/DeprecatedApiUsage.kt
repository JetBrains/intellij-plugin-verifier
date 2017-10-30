package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.misc.impossible
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption.SIMPLE_TYPE
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation

/**
 * @author Sergey Patrikeev
 */
abstract class DeprecatedApiUsage {
  abstract val deprecatedElement: Location

  abstract val usageLocation: Location

  abstract val shortDescription: String

  abstract val fullDescription: String

  final override fun equals(other: Any?): Boolean = other is DeprecatedApiUsage && fullDescription == other.fullDescription

  final override fun hashCode(): Int = fullDescription.hashCode()

  final override fun toString(): String = fullDescription
}

fun Location.formatUsageLocation(): String = when (this) {
  is ClassLocation -> formatClassLocation(FULL_NAME, NO_GENERICS)
  is MethodLocation -> formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, SIMPLE_RETURN_TYPE_CLASS_NAME)
  is FieldLocation -> formatFieldLocation(FULL_HOST_NAME, SIMPLE_TYPE)
  else -> impossible()
}
