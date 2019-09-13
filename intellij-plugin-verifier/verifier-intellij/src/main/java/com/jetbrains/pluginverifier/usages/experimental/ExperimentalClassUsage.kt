package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

class ExperimentalClassUsage(
    override val apiReference: ClassReference,
    override val apiElement: ClassLocation,
    override val usageLocation: Location
) : ExperimentalApiUsage() {

  override val shortDescription
    get() = "Experimental API " + apiElement.elementType.presentableName + " ${apiElement.formatClassLocation(FULL_NAME, NO_GENERICS)} reference"

  override val fullDescription: String
    get() = buildString {
      append("Experimental API " + apiElement.elementType.presentableName + " ${apiElement.formatClassLocation(FULL_NAME, WITH_GENERICS)}")
      append(" is referenced in " + usageLocation.formatUsageLocation())
      append(". This ")
      append(apiElement.elementType.presentableName)
      append(" can be changed in a future release leading to incompatibilities")
    }

  override fun equals(other: Any?) = other is ExperimentalClassUsage
      && apiReference == other.apiReference
      && apiElement == other.apiElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiReference, apiElement, usageLocation)
}