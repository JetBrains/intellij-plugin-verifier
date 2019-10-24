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

  override val shortDescription
    get() = "Internal " + apiElement.elementType.presentableName + " ${apiElement.formatClassLocation(FULL_NAME, NO_GENERICS)} reference"

  override val fullDescription: String
    get() = buildString {
      append("Internal " + apiElement.elementType.presentableName + " ${apiElement.formatClassLocation(FULL_NAME, WITH_GENERICS)}")
      append(" is referenced in " + usageLocation.formatUsageLocation())
      append(". This " + apiElement.elementType.presentableName)
      append(
        " is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates " +
          "that the class is not supposed to be used in client code."
      )
    }

  override fun equals(other: Any?) = other is InternalClassUsage
    && apiReference == other.apiReference
    && apiElement == other.apiElement
    && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiReference, apiElement, usageLocation)
}