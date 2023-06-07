/*
 * Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering

import com.jetbrains.plugin.structure.intellij.plugin.PluginVendors
import com.jetbrains.pluginverifier.usages.ApiUsage
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext

class InternalApiUsageFilter : ApiUsageFilter {
  override fun shouldReport(apiUsage: ApiUsage, context: VerificationContext): ApiUsageFilter.Result {
    return when {
      apiUsage is InternalApiUsage
        && context is PluginVerificationContext
        && PluginVendors.isDevelopedByJetBrains(context.idePlugin) ->
        ApiUsageFilter.Result.Ignore("Internal API usage from internal plugins is allowed.")
      else -> ApiUsageFilter.Result.Report
    }
  }
}