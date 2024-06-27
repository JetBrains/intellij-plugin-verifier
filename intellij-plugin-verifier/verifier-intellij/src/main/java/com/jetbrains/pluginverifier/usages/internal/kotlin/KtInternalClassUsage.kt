package com.jetbrains.pluginverifier.usages.internal.kotlin

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

class KtInternalClassUsage(
  override val apiReference: ClassReference,
  override val apiElement: ClassLocation,
  override val usageLocation: Location
) : KtInternalModifierUsage() {

  override val problemType: String
    get() = "Usage of class having Kotlin `internal` visibility modifier."

  override val shortDescription
    get() = "Internal " + apiElement.elementType.presentableName + " ${apiElement.formatClassLocation(FULL_NAME, NO_GENERICS)} reference"

  override val fullDescription: String
    get() = buildString {
      val element = apiElement.elementType.presentableName
      append("Internal $element ${apiElement.formatClassLocation(FULL_NAME, WITH_GENERICS)}")
      append(" is referenced in " + usageLocation.formatUsageLocation())
      append(". This $element is marked with Kotlin `internal` visibility modifier, indicating ")
      append("that it is not supposed to be referenced in client code outside the declaring module.")
    }

  override fun equals(other: Any?) = other is KtInternalModifierUsage
    && apiReference == other.apiReference
    && apiElement == other.apiElement
    && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiReference, apiElement, usageLocation)
}