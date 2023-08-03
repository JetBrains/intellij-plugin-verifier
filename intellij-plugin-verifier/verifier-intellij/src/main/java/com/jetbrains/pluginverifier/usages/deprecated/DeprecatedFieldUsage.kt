/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

class DeprecatedFieldUsage(
  override val apiReference: FieldReference,
  override val apiElement: FieldLocation,
  override val usageLocation: Location,
  deprecationInfo: DeprecationInfo
) : DeprecatedApiUsage(deprecationInfo) {

  override val problemType: String
    get() = "Deprecated field usage"

  override val shortDescription
    get() = "Deprecated field ${apiElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.NO_TYPE)} access"

  override val fullDescription
    get() = buildString {
      append("Deprecated field ${apiElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.FULL_TYPE)} is")
      append(" accessed in ${usageLocation.formatUsageLocation()}")
      if (deprecationInfo.forRemoval) {
        append(". This field will be removed in ")
        append(deprecationInfo.untilVersion ?: "a future release")
      }
    }

  override fun equals(other: Any?) = other is DeprecatedFieldUsage
    && apiReference == other.apiReference
    && apiElement == other.apiElement
    && usageLocation == other.usageLocation
    && deprecationInfo == other.deprecationInfo

  override fun hashCode() = Objects.hash(apiReference, apiElement, usageLocation, deprecationInfo)
}