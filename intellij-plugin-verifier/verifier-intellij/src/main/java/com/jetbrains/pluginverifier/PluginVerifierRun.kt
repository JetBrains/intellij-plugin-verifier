/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFICATION_TIME
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.base.utils.ExecutorWithProgress
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import java.time.Duration

fun runSeveralVerifiers(reportage: PluginVerificationReportage, verifiers: List<PluginVerifier>): List<PluginVerificationResult> {
  if (verifiers.isEmpty()) {
    return emptyList()
  }

  val executor = ExecutorWithProgress<PluginVerificationResult>("verifier", getConcurrencyLevel(), true) { progressData ->
    val result = progressData.result!!
    reportage.logVerificationStage(
      "Finished ${progressData.finishedNumber} of ${progressData.totalNumber} verifications (in ${String.format("%.1f", progressData.elapsedTime.toDouble() / 1000)} s): " +
        "${result.verificationTarget} against ${result.plugin}: ${result.verificationVerdict}"
    )
    reportage.reportTelemetry(result.plugin, PluginTelemetry(PLUGIN_VERIFICATION_TIME to Duration.ofMillis(progressData.elapsedTime)))
  }

  val tasks = verifiers.map { verifier ->
    ExecutorWithProgress.Task(verifier.verificationDescriptor.toString()) {
      val verificationResult = verifier.loadPluginAndVerify()
      reportage.reportVerificationResult(verificationResult)
      verificationResult
    }
  }
  return executor.executeTasks(tasks)
}

fun getConcurrencyLevel(): Int {
  val fromProperty = System.getProperty("intellij.plugin.verifier.concurrency.level")?.toIntOrNull()
  if (fromProperty != null) {
    check(fromProperty > 0) { "Invalid concurrency level: $fromProperty" }
    return fromProperty
  }

  val availableMemory = Runtime.getRuntime().maxMemory()
  val availableCpu = Runtime.getRuntime().availableProcessors().toLong()
  //About 200 Mb is needed for an average verification
  val maxByMemory = availableMemory / 1024 / 1024 / 200
  return maxOf(8, minOf(maxByMemory, availableCpu)).toInt()
}