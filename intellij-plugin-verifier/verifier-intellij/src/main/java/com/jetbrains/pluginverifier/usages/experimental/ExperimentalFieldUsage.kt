/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

class ExperimentalFieldUsage(
  override val apiReference: FieldReference,
  override val apiElement: FieldLocation,
  override val usageLocation: Location
) : ExperimentalApiUsage() {

  override val problemType: String
    get() = "Experimental field usage"

  override val shortDescription
    get() = "Experimental API field ${apiElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.NO_TYPE)} access"

  override val fullDescription
    get() = buildString {
      append("Experimental API field ${apiElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.FULL_TYPE)} is")
      append(" accessed in ${usageLocation.formatUsageLocation()}")
      append(". This field can be changed in a future release leading to incompatibilities")
    }

  override fun equals(other: Any?) = other is ExperimentalFieldUsage
    && apiReference == other.apiReference
    && apiElement == other.apiElement
    && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiReference, apiElement, usageLocation)
}