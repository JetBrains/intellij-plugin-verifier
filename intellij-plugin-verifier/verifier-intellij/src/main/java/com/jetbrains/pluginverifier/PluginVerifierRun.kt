/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFICATION_TIME
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.base.utils.ExecutorWithProgress
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import java.time.Duration
import kotlin.system.measureTimeMillis

/**
 * Runs the [verifiers] concurrently and reports each result to the [reportage] as soon as it is available.
 *
 * All results are kept in memory until every verification finishes. The [retain] function is applied to each result
 * right after it has been reported, and its outcome is what is kept and returned. Use it to drop data
 * that is no longer needed once reported, such as the full dependencies graph, see [retainDirectDependencies].
 */
fun runSeveralVerifiers(
  reportage: PluginVerificationReportage,
  verifiers: List<PluginVerifier>,
  retain: (PluginVerificationResult) -> PluginVerificationResult = { it }
): List<PluginVerificationResult> {
  if (verifiers.isEmpty()) {
    return emptyList()
  }

  val executor = ExecutorWithProgress<PluginVerificationResult>("verifier", getConcurrencyLevel(), true) { progressData ->
    val result = progressData.result!!
    reportage.logVerificationStage(
      "Finished ${progressData.finishedNumber} of ${progressData.totalNumber} verifications (in ${String.format("%.1f", progressData.elapsedTime.toDouble() / 1000)} s): " +
        "${result.verificationTarget} against ${result.plugin}: ${result.verificationVerdict}"
    )
  }

  val batchContext = PluginVerifierBatchContext()

  val tasks = verifiers.map { verifier ->
    ExecutorWithProgress.Task(verifier.verificationDescriptor.toString()) {
      val verificationResult: PluginVerificationResult
      measureTimeMillis {
        verificationResult = verifier.loadPluginAndVerify(batchContext)
      }.let { verificationTime ->
        reportage.reportTelemetry(verificationResult.plugin, PluginTelemetry(PLUGIN_VERIFICATION_TIME to Duration.ofMillis(verificationTime)))
        if (verificationResult is PluginVerificationResult.Verified) {
          reportage.reportTelemetry(verificationResult.plugin, verificationResult.telemetry)
        }
        reportage.reportVerificationResult(verificationResult)
        retain(verificationResult)
      }
    }
  }
  return executor.executeTasks(tasks)
}
