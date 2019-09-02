package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.runSeveralVerifiers
import com.jetbrains.pluginverifier.tasks.Task
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter

class CheckIdeTask(private val parameters: CheckIdeParams) : Task {

  override fun execute(
      reportage: PluginVerificationReportage,
      pluginDetailsCache: PluginDetailsCache
  ): CheckIdeResult {
    with(parameters) {
      val verifiers = verificationDescriptors.map {
        PluginVerifier(
            it,
            problemsFilters,
            pluginDetailsCache,
            listOf(DynamicallyLoadedFilter())
        )
      }

      val results = runSeveralVerifiers(reportage, verifiers)

      return CheckIdeResult(
          verificationTarget,
          results,
          missingCompatibleVersionsProblems
      )
    }
  }

}

