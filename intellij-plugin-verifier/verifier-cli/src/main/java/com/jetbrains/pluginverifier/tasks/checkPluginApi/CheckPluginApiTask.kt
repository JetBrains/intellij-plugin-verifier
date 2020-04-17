/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.runSeveralVerifiers
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.tasks.twoTargets.TwoTargetsVerificationResults
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter

class CheckPluginApiTask(private val parameters: CheckPluginApiParams) : Task {

  override fun execute(
    reportage: PluginVerificationReportage,
    pluginDetailsCache: PluginDetailsCache
  ): TwoTargetsVerificationResults {
    with(parameters) {
      val verifiers = arrayListOf<PluginVerifier>()
      verifiers += baseVerificationDescriptors.map {
        PluginVerifier(
          it,
          problemsFilters,
          pluginDetailsCache,
          listOf(DynamicallyLoadedFilter())
        )
      }

      verifiers += newVerificationDescriptors.map {
        PluginVerifier(
          it,
          problemsFilters,
          pluginDetailsCache,
          listOf(DynamicallyLoadedFilter())
        )
      }

      val results = runSeveralVerifiers(reportage, verifiers)
      return TwoTargetsVerificationResults(
        baseVerificationTarget,
        results.filter { it.verificationTarget == baseVerificationTarget },
        newVerificationTarget,
        results.filter { it.verificationTarget == newVerificationTarget }
      )
    }
  }

}
