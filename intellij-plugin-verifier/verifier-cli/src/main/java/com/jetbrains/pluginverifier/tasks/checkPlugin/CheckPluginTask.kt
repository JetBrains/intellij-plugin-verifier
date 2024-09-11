/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.filtering.ApiUsageFilter
import com.jetbrains.pluginverifier.filtering.InternalApiUsageFilter
import com.jetbrains.pluginverifier.filtering.KtInternalModifierUsageFilter
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.runSeveralVerifiers
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter

/**
 * The 'check-plugin' task verifies many plugins against many IDEs.
 *
 * If one verified plugins depends on
 * another verified plugin then the [dependency resolution] [DependencyFinder]
 * prefers the verified plugin to a plugin from the [PluginRepository].
 */
class CheckPluginTask(private val parameters: CheckPluginParams) : Task {

  override fun execute(
    reportage: PluginVerificationReportage,
    pluginDetailsCache: PluginDetailsCache
  ): CheckPluginResult {
    with(parameters) {
      val verifiers = verificationDescriptors.map {
        PluginVerifier(
          it,
          problemsFilters,
          pluginDetailsCache,
          listOf(DynamicallyLoadedFilter()),
          excludeExternalBuildClassesSelector,
          parameters.resolveApiUsageFilters
        )
      }

      val results = runSeveralVerifiers(reportage, verifiers)
      val ideDescriptorsWithInvalidPluginFiles = ideDescriptors.associateWith { invalidPluginFiles }
      return CheckPluginResult(invalidPluginFiles, results, ideDescriptorsWithInvalidPluginFiles)
    }
  }

  private val CheckPluginParams.resolveApiUsageFilters: List<ApiUsageFilter>
    get() = if (internalApiVerificationMode == InternalApiVerificationMode.IGNORE_IN_JETBRAINS_PLUGINS) {
      listOf(InternalApiUsageFilter())
    } else {
      emptyList()
    } + KtInternalModifierUsageFilter()
}
