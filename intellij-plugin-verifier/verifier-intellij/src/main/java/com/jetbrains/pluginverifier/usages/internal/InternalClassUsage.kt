/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

class InternalClassUsage(
  override val apiReference: ClassReference,
  override val apiElement: ClassLocation,
  override val usageLocation: Location
) : InternalApiUsage() {

  override val problemType: String
    get() = "Internal class usage"

  override val shortDescription
    get() = "Internal " + apiElement.elementType.presentableName + " ${apiElement.formatClassLocation(FULL_NAME, NO_GENERICS)} reference"

  override val fullDescription: String
    get() = buildString {
      append("Internal " + apiElement.elementType.presentableName + " ${apiElement.formatClassLocation(FULL_NAME, WITH_GENERICS)}")
      append(" is referenced in " + usageLocation.formatUsageLocation())
      append(". This " + apiElement.elementType.presentableName)
      append(
        " is marked with ${InternalConstants.INTERNAL_API_ANNOTATION} annotation or " +
          "${InternalConstants.INTELLIJ_INTERNAL_API_ANNOTATION} annotation and indicates " +
          "that the class is not supposed to be used in client code."
      )
    }

  override fun equals(other: Any?) = other is InternalClassUsage
    && apiReference == other.apiReference
    && apiElement == other.apiElement
    && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiReference, apiElement, usageLocation)
}