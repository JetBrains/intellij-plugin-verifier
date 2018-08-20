package com.jetbrains.pluginverifier.results.experimental

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.usage.formatUsageLocation
import java.util.*

class ExperimentalClassUsage(
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
      && apiElement == other.apiElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiElement, usageLocation)

  companion object {
    private const val serialVersionUID = 0L
  }
}