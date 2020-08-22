package com.jetbrains.pluginverifier.usages.properties

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning

data class DeprecatedPropertyUsageWarning(
  val propertyKey: String,
  val originalResourceBundle: String,
  val deprecatedResourceBundle: String,
  val usageLocation: Location
) : CompatibilityWarning() {

  override val shortDescription: String
    get() = "Reference to a deprecated property {0} of resource bundle {1}, which was moved to {2}".formatMessage(
      propertyKey, originalResourceBundle, deprecatedResourceBundle
    )

  override val fullDescription: String
    get() = (
      "{0} {1} references deprecated property {2} that was moved from the resource bundle {3} to {4}. " +
        "The clients will continue to get the correct value of the property but they are encouraged " +
        "to place the property to their own resource bundle"
      ).formatMessage(
        usageLocation.elementType.presentableName.capitalize(),
        usageLocation,
        propertyKey,
        originalResourceBundle,
        deprecatedResourceBundle
      )
}