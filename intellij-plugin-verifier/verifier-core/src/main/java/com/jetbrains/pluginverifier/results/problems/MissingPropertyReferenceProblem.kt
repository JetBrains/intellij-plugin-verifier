/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.base.utils.formatMessage
import com.jetbrains.pluginverifier.results.location.Location
import java.util.*

class MissingPropertyReferenceProblem(
  val propertyKey: String,
  val bundleBaseName: String,
  val usageLocation: Location
) : CompatibilityProblem() {

  override val problemType
    get() = "Missing property reference"

  override val shortDescription
    get() = "Reference to a missing property {0} of resource bundle {1}".formatMessage(propertyKey, bundleBaseName)

  override val fullDescription: String
    get() = "{0} {1} references property {2} that is not found in resource bundle {3}. This can lead to **MissingResourceException** exception at runtime.".formatMessage(
      usageLocation.elementType.presentableName.capitalize(),
      usageLocation,
      propertyKey,
      bundleBaseName
    )

  override fun equals(other: Any?) = other is MissingPropertyReferenceProblem
    && propertyKey == other.propertyKey
    && bundleBaseName == other.bundleBaseName
    && usageLocation == other.usageLocation

  override fun hashCode() = Objects.hash(propertyKey, bundleBaseName, usageLocation)
}