package com.jetbrains.pluginverifier.results.experimental

import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.FieldTypeOption
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.formatFieldLocation
import com.jetbrains.pluginverifier.results.usage.formatUsageLocation
import java.util.*

class ExperimentalFieldUsage(
    override val apiElement: FieldLocation,
    override val usageLocation: Location
) : ExperimentalApiUsage() {

  override val shortDescription
    get() = "Experimental API field ${apiElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.NO_TYPE)} access"

  override val fullDescription
    get() = buildString {
      append("Experimental API field ${apiElement.formatFieldLocation(FULL_HOST_NAME, FieldTypeOption.FULL_TYPE)} is")
      append(" accessed in ${usageLocation.formatUsageLocation()}")
      append(". This field can be changed in a future release leading to incompatibilities")
    }

  override fun equals(other: Any?) = other is ExperimentalFieldUsage
      && apiElement == other.apiElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiElement, usageLocation)

  companion object {
    private const val serialVersionUID = 0L
  }
}