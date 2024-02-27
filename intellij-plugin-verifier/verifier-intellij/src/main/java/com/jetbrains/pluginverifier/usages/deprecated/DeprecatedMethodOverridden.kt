/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.location.toReference
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
  override val apiElement: MethodLocation,
  override val usageLocation: MethodLocation,
  deprecationInfo: DeprecationInfo
) : DeprecatedApiUsage(deprecationInfo) {

  override val problemType: String
    get() = "Deprecated method is overridden"

  override val apiReference
    get() = apiElement.toReference()

  override val shortDescription
    get() = "Deprecated method ${apiElement.formatMethodLocation(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, NO_RETURN_TYPE, NO_PARAMETER_NAMES)} is overridden"

  override val fullDescription
    get() = buildString {
      append("Deprecated method ${apiElement.formatMethodLocation(FULL_HOST_NAME, FULL_PARAM_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE)}")
      append(" is overridden in class ${usageLocation.hostClass.formatClassLocation(FULL_NAME, NO_GENERICS)}")
      if (deprecationInfo.forRemoval) {
        append(". This method will be removed in ")
        append(deprecationInfo.untilVersion ?: "a future release")
      }
    }

  override fun equals(other: Any?) = other is DeprecatedMethodOverridden
    && apiReference == other.apiReference
    && apiElement == other.apiElement
    && usageLocation == other.usageLocation
    && deprecationInfo == other.deprecationInfo

  override fun hashCode() = Objects.hash(apiReference, apiElement, usageLocation, deprecationInfo)
}