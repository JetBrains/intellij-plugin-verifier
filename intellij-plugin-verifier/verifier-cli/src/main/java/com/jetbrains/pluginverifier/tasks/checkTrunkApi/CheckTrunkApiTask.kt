/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.runSeveralVerifiers
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.tasks.sortWithBigPluginsInFront
import com.jetbrains.pluginverifier.tasks.twoTargets.TwoTargetsVerificationResults
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter

/**
 * The 'check-trunk-api' task that runs the verification of a trunk and a release IDEs and reports the new API breakages.
 */
class CheckTrunkApiTask(private val parameters: CheckTrunkApiParams) : Task {

  override fun execute(
    reportage: PluginVerificationReportage,
    pluginDetailsCache: PluginDetailsCache
  ): TwoTargetsVerificationResults {
    with(parameters) {
      val classFilters = listOf(DynamicallyLoadedFilter())

      val verifiers = arrayListOf<PluginVerifier>()
      verifiers += releaseVerificationDescriptors.map {
        PluginVerifier(
          it,
          problemsFilters,
          pluginDetailsCache,
          classFilters,
          excludeExternalBuildClassesSelector
        )
      }

      verifiers += trunkVerificationDescriptors.map {
        PluginVerifier(
          it,
          problemsFilters,
          pluginDetailsCache,
          classFilters,
          excludeExternalBuildClassesSelector
        )
      }

      val sortedVerifiers = verifiers.sortWithBigPluginsInFront()
      val results = runSeveralVerifiers(reportage, sortedVerifiers)

      return TwoTargetsVerificationResults(
        releaseVerificationTarget,
        results.filter { it.verificationTarget == releaseVerificationTarget },
        trunkVerificationTarget,
        results.filter { it.verificationTarget == trunkVerificationTarget }
      )
    }
  }

}