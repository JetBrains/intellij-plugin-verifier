/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.nonExtendable

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.ElementType
import com.jetbrains.pluginverifier.results.location.toReference
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

class NonExtendableTypeInherited(
  override val apiElement: ClassLocation,
  override val usageLocation: ClassLocation
) : NonExtendableApiUsage() {

  override val problemType: String
    get() = "Non-extendable type inherited"

  override val apiReference
    get() = apiElement.toReference()

  private val inheritanceVerb: String
    get() = when {
      apiElement.elementType == ElementType.INTERFACE && usageLocation.elementType == ElementType.INTERFACE -> "extended"
      apiElement.elementType == ElementType.CLASS && usageLocation.elementType == ElementType.CLASS -> "extended"
      apiElement.elementType == ElementType.INTERFACE && usageLocation.elementType == ElementType.CLASS -> "implemented"
      else -> "inherited"
    }

  override val shortDescription
    get() = "Non-extendable " + apiElement.elementType.presentableName + " ${apiElement.formatClassLocation(FULL_NAME, NO_GENERICS)} is $inheritanceVerb"

  override val fullDescription: String
    get() = buildString {
      append("Non-extendable " + apiElement.elementType.presentableName + " ${apiElement.formatClassLocation(FULL_NAME, WITH_GENERICS)} is $inheritanceVerb by ")
      append(usageLocation.formatUsageLocation() + ". ")
      append("This " + apiElement.elementType.presentableName)
      append(" is marked with @org.jetbrains.annotations.ApiStatus.NonExtendable, which indicates that the ")
      append(apiElement.elementType.presentableName)
      append(" is not supposed to be extended. See documentation of the @ApiStatus.NonExtendable for more info.")
    }

  override fun equals(other: Any?) = other is NonExtendableTypeInherited
    && apiElement == other.apiElement
    && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiElement, usageLocation)
}