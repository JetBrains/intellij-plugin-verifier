/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.NO_PARAMETER_NAMES
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.NO_RETURN_TYPE
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsage
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

/**
 * Violating invocation of `ApiStatus.OverrideOnly` method.
 */
class OverrideOnlyMethodUsage(
  override val apiReference: MethodReference,
  override val apiElement: MethodLocation,
  override val usageLocation: Location
) : ApiUsage() {

  override val problemType: String
    get() = "Violating invocation of `ApiStatus.OverrideOnly` method"

  override val shortDescription
    get() = "Invocation of override-only method " + apiElement.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, NO_RETURN_TYPE, NO_PARAMETER_NAMES)

  override val fullDescription
    get() = buildString {
      append("Override-only method " + apiElement.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, NO_RETURN_TYPE, NO_PARAMETER_NAMES))
      append(" is invoked in " + usageLocation.formatUsageLocation() + ". ")
      append(
        "This method is marked with @org.jetbrains.annotations.ApiStatus.OverrideOnly annotation, " +
          "which indicates that the method must be only overridden but not invoked by client code. " +
          "See documentation of the @ApiStatus.OverrideOnly for more info."
      )
    }

  override fun equals(other: Any?) = other is OverrideOnlyMethodUsage
    && apiReference == other.apiReference
    && apiElement == other.apiElement
    && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiReference, apiElement, usageLocation)
}