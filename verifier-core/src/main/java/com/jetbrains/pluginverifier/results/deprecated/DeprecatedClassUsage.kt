package com.jetbrains.pluginverifier.results.deprecated

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.NO_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption.WITH_GENERICS
import com.jetbrains.pluginverifier.results.presentation.ClassOption.FULL_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.usage.formatUsageLocation
import java.util.*

class DeprecatedClassUsage(
    override val apiElement: ClassLocation,
    override val usageLocation: Location,
    deprecationInfo: DeprecationInfo
) : DeprecatedApiUsage(deprecationInfo) {
  override val shortDescription
    get() = "Deprecated " + apiElement.elementType.presentableName + " ${apiElement.formatClassLocation(FULL_NAME, NO_GENERICS)} reference"

  override val fullDescription: String
    get() = buildString {
      append("Deprecated " + apiElement.elementType.presentableName + " ${apiElement.formatClassLocation(FULL_NAME, WITH_GENERICS)}")
      append(" is referenced in " + usageLocation.formatUsageLocation())
      if (deprecationInfo.forRemoval) {
        append(". This " + apiElement.elementType.presentableName + " will be removed in ")
        append(deprecationInfo.untilVersion ?: " a future release")
      }
    }

  override fun equals(other: Any?) = other is DeprecatedClassUsage
      && apiElement == other.apiElement
      && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(apiElement, usageLocation)

  companion object {
    private const val serialVersionUID = 0L
  }
}