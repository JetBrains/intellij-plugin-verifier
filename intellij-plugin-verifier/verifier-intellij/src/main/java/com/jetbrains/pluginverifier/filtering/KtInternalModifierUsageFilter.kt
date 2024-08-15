/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering

import com.jetbrains.pluginverifier.filtering.ApiUsageFilter.Result.Ignore
import com.jetbrains.pluginverifier.filtering.ApiUsageFilter.Result.Report
import com.jetbrains.pluginverifier.usages.ApiUsage
import com.jetbrains.pluginverifier.usages.internal.kotlin.KtInternalModifierUsage
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import com.jetbrains.pluginverifier.verifiers.resolution.FullyQualifiedClassName
import com.jetbrains.pluginverifier.verifiers.resolution.toFullyQualifiedClassName

private val ignoredPackages = listOf(
  "kotlin.jvm.internal",
  "kotlin.internal",
  "kotlin.coroutines.jvm.internal",
  "kotlinx.serialization.internal"
)

private val ignoredClasses: Set<FullyQualifiedClassName> = setOf(
  "kotlin._Assertions"
)

class KtInternalModifierUsageFilter : ApiUsageFilter {
  override fun shouldReport(apiUsage: ApiUsage, context: VerificationContext): ApiUsageFilter.Result =
    when {
      apiUsage is KtInternalModifierUsage && apiUsage.isIgnored() -> Ignore("Kotlin internal visibility modifier in '${apiUsage.packageName}' package is ignored.")
      else -> Report
    }

  private fun ApiUsage.isIgnored(): Boolean {
    return hasIgnoredClass() || hasIgnoredPackage()
  }

  private fun ApiUsage.hasIgnoredClass(): Boolean {
    val apiUsageClass: BinaryClassName = apiElement.containingClass.className
    return ignoredClasses.contains(apiUsageClass.toFullyQualifiedClassName())
  }

  private fun ApiUsage.hasIgnoredPackage(): Boolean {
    val apiPackage = this.packageName
    return ignoredPackages
      .any {
        apiPackage.startsWith(it)
      }
  }

  private val ApiUsage.packageName: String
    get() = apiElement.containingClass.packageName.replace('/', '.')
}