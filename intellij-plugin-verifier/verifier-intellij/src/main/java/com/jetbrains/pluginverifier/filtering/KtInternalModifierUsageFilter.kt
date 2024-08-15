/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering

import com.jetbrains.pluginverifier.filtering.ApiUsageFilter.Result.Ignore
import com.jetbrains.pluginverifier.filtering.ApiUsageFilter.Result.Report
import com.jetbrains.pluginverifier.usages.ApiUsage
import com.jetbrains.pluginverifier.usages.internal.kotlin.KtInternalModifierUsage
import com.jetbrains.pluginverifier.verifiers.VerificationContext

private val kotlinPlatformPackages = listOf("kotlin", "kotlinx")

class KtInternalModifierUsageFilter(private val excludedPackages: List<String> = kotlinPlatformPackages) : ApiUsageFilter {
  override fun shouldReport(apiUsage: ApiUsage, context: VerificationContext): ApiUsageFilter.Result =
    when {
      apiUsage is KtInternalModifierUsage && apiUsage.isKotlinPlatform() -> Ignore("Kotlin internal visibility modifier in '${apiUsage.packageName}' package is ignored.")
      else -> Report
    }

  private fun ApiUsage.isKotlinPlatform(): Boolean {
    val apiPackage = this.packageName
    return excludedPackages
      .map { "$it." }
      .any {
        apiPackage.startsWith(it)
      }
  }

  private val ApiUsage.packageName: String
    get() = apiElement.containingClass.packageName.replace('/', '.')
}