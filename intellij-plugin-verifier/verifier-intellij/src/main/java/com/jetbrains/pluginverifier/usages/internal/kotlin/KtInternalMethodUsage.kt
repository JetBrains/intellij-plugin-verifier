/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.internal.kotlin

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
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

class KtInternalMethodUsage(
  override val apiReference: MethodReference,
  override val apiElement: MethodLocation,
  override val usageLocation: Location
) : KtInternalModifierUsage() {

  override val problemType: String
    get() = "Kotlin Internal method usage"

  override val shortDescription
    get() = "Internal " + apiElement.elementType.presentableName + " ${apiElement.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, NO_RETURN_TYPE, NO_PARAMETER_NAMES)} invocation"

  override val fullDescription
    get() = buildString {
      val element = apiElement.elementType.presentableName
      append("Internal $element ")
      append(apiElement.formatMethodLocation(FULL_HOST_NAME, FULL_PARAM_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE))
      append(" is invoked in ${usageLocation.formatUsageLocation()}")
      append(". This $element is marked with Kotlin `internal` visibility modifier, indicating ")
      append("that it is not supposed to be invoked by the client code outside the declaring module."
      )
    }

  override fun equals(other: Any?) = other is KtInternalMethodUsage
    && apiReference == other.apiReference
    && apiElement == other.apiElement
    && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiReference, apiElement, usageLocation)
}