/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.properties

import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.utils.getBundleBaseName
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.problems.MissingPropertyReferenceProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import java.util.*

interface PropertyChecker {
  fun checkProperty(
    resourceBundleName: String,
    propertyKey: String,
    context: VerificationContext,
    usageLocation: Location
  )
}

object DefaultPropertyChecker: PropertyChecker {
  override fun checkProperty(
    resourceBundleName: String,
    propertyKey: String,
    context: VerificationContext,
    usageLocation: Location
  ) {
    if (resourceBundleName != getBundleBaseName(resourceBundleName)) {
      //In general, we can't resolve non-base bundles, like "some.Bundle_en" because we don't know the locale to use.
      return
    }

    val resolutionResult = context.classResolver.resolveExactPropertyResourceBundle(resourceBundleName, Locale.ROOT)
    if (resolutionResult !is ResolutionResult.Found) {
      return
    }

    val resourceBundle = resolutionResult.value
    if (resourceBundle.containsKey(propertyKey)) return

    // MP-3201: Don't report warnings about properties which were moved to *DeprecatedMessagesBundle files
    val deprecatedBundleNames = context.classResolver.allBundleNameSet.baseBundleNames
      .filter { it.endsWith("DeprecatedMessagesBundle") }
    for (deprecatedBundleName in deprecatedBundleNames) {
      val resolution = context.classResolver.resolveExactPropertyResourceBundle(deprecatedBundleName, Locale.ROOT)
      if (resolution is ResolutionResult.Found) {
        val deprecatedBundle = resolution.value
        if (deprecatedBundle.containsKey(propertyKey)) {
          context.warningRegistrar.registerCompatibilityWarning(
            DeprecatedPropertyUsageWarning(propertyKey, resourceBundleName, deprecatedBundleName, usageLocation)
          )
          return
        }
      }
    }

    context.problemRegistrar.registerProblem(
      MissingPropertyReferenceProblem(
        propertyKey,
        resourceBundleName,
        usageLocation
      )
    )
  }
}