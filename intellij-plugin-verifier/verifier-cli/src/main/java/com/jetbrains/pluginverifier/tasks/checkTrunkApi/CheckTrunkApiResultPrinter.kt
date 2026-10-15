/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter
import com.jetbrains.pluginverifier.tasks.twoTargets.TwoTargetsResultPrinter

const val INTERNAL_API_USAGES_SUMMARY_FILE = "internal-api-usages-summary.json"

class CheckTrunkApiResultPrinter : TaskResultPrinter {
  override fun printResults(taskResult: TaskResult, outputOptions: OutputOptions) {
    val result = taskResult as CheckTrunkApiVerificationResults
    val summary = InternalApiUsagesSummary.from(result.trunkTarget, result.trunkResults)

    val trunkReportDirectory = outputOptions.getTargetReportDirectory(result.trunkTarget)
    val verificationReportsRoot = requireNotNull(trunkReportDirectory.parent) {
      "The trunk verification report directory must be nested under the verification reports root: $trunkReportDirectory"
    }
    val summaryPath = verificationReportsRoot.resolve(INTERNAL_API_USAGES_SUMMARY_FILE)
    summary.writeTo(summaryPath)
    println("Internal API usages summary has been saved to $summaryPath")

    outputOptions.teamCityLog?.let { teamCityLog ->
      summary.reportTeamCityStatistics(teamCityLog)
      teamCityLog.publishArtifacts("$summaryPath => .")
    }

    // Preserve the existing check-trunk-api output (TeamCity tests, HTML, Markdown) unchanged.
    TwoTargetsResultPrinter().printResults(result.asTwoTargetsVerificationResults(), outputOptions)
  }
}

private fun InternalApiUsagesSummary.reportTeamCityStatistics(teamCityLog: TeamCityLog) {
  teamCityLog.buildStatisticValue("intellij.plugin.verifier.internal.api.plugins.checked", totals.checkedPluginCount)
  teamCityLog.buildStatisticValue("intellij.plugin.verifier.internal.api.plugins.invalid", totals.invalidPluginCount)
  teamCityLog.buildStatisticValue("intellij.plugin.verifier.internal.api.plugins.notFound", totals.notFoundPluginCount)
  teamCityLog.buildStatisticValue("intellij.plugin.verifier.internal.api.plugins.failedToDownload", totals.failedToDownloadPluginCount)
  teamCityLog.buildStatisticValue("intellij.plugin.verifier.internal.api.plugins.affected", totals.affectedPluginCount)
  teamCityLog.buildStatisticValue("intellij.plugin.verifier.internal.api.elements.distinct", totals.distinctApiElementCount)
  teamCityLog.buildStatisticValue("intellij.plugin.verifier.internal.api.usages.total", totals.usageCount)
  teamCityLog.buildStatisticValue("intellij.plugin.verifier.internal.api.usages.nonignored", totals.nonIgnoredUsageCount)
  teamCityLog.buildStatisticValue("intellij.plugin.verifier.internal.api.usages.ignored", totals.ignoredUsageCount)
  teamCityLog.buildStatisticValue("intellij.plugin.verifier.internal.api.top.element.plugins", apiElements.firstOrNull()?.affectedPluginCount ?: 0)
}
