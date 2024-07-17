/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.internal.kotlin

import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

class KtInternalFieldUsage(
  override val apiReference: FieldReference,
  override val apiElement: FieldLocation,
  override val usageLocation: Location
) : KtInternalModifierUsage() {

  override val problemType: String
    get() = "Kotlin Internal field usage"

  override val shortDescription
    get() = "Internal field ${apiElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.NO_TYPE)} access"

  override val fullDescription
    get() = buildString {
      val element = apiElement.elementType.presentableName
      append("Internal $element ${apiElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.FULL_TYPE)} is")
      append(" accessed in ${usageLocation.formatUsageLocation()}")
      append(". This $element is marked with Kotlin `internal` visibility modifier, indicating ")
      append("that it is not supposed to be used in client code outside the declaring module.")
    }

  override fun equals(other: Any?) = other is KtInternalFieldUsage
    && apiReference == other.apiReference
    && apiElement == other.apiElement
    && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiReference, apiElement, usageLocation)
}