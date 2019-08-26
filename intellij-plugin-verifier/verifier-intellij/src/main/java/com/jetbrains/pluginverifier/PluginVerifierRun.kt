package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.base.utils.ExecutorWithProgress
import com.jetbrains.pluginverifier.reporting.PluginReporters
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import java.util.concurrent.Callable

fun runSeveralVerifiers(reportage: PluginVerificationReportage, verifiers: List<PluginVerifier>): List<PluginVerificationResult> {
  if (verifiers.isEmpty()) {
    return emptyList()
  }

  val executor = ExecutorWithProgress<PluginVerificationResult>("verifier", getConcurrencyLevel()) { progressData ->
    val result = progressData.result
    reportage.logVerificationStage(
        "Finished ${progressData.finishedNumber} of ${progressData.totalNumber} verifications: " +
            "${result.verificationTarget} against ${result.plugin}: ${result.verificationVerdict}"
    )
  }

  val tasks = verifiers.map { verifier ->
    Callable {
      reportage.createPluginReporters(verifier.plugin, verifier.verificationTarget).use { reporters ->
        val verificationResult = verifier.loadPluginAndVerify()
        reporters.reportResults(verificationResult)
        verificationResult
      }
    }
  }
  return executor.executeTasks(tasks)
}

private fun getConcurrencyLevel(): Int {
  val fromProperty = System.getProperty("intellij.plugin.verifier.concurrency.level")?.toIntOrNull()
  if (fromProperty != null) {
    check(fromProperty > 0) { "Invalid concurrency level: $fromProperty" }
    return fromProperty
  }

  val availableMemory = Runtime.getRuntime().maxMemory()
  val availableCpu = Runtime.getRuntime().availableProcessors().toLong()
  //About 200 Mb is needed for an average verification
  val maxByMemory = availableMemory / 1024 / 1024 / 200
  return maxOf(4, minOf(maxByMemory, availableCpu)).toInt()
}

private fun PluginReporters.reportResults(result: PluginVerificationResult) {
  reportVerificationResult(result)

  if (result is PluginVerificationResult.Verified) {
    result.compatibilityWarnings.forEach { reportNewWarningDetected(it) }
    result.compatibilityProblems.forEach { reportNewProblemDetected(it) }
    result.deprecatedUsages.forEach { reportDeprecatedUsage(it) }
    result.experimentalApiUsages.forEach { reportExperimentalApi(it) }
    reportDependencyGraph(result.dependenciesGraph)

    for ((problem, reason) in result.ignoredProblems) {
      reportProblemIgnored(
          ProblemIgnoredEvent(
              result.plugin,
              result.verificationTarget,
              problem,
              reason
          )
      )
    }
  }
}