package com.jetbrains.pluginverifier.core

import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.results.Result
import org.apache.commons.io.FileUtils

/**
 * @author Sergey Patrikeev
 */
object Verification {

  private val AVERAGE_VERIFIER_MEMORY = 200 * FileUtils.ONE_MB

  fun run(verifierParameters: VerifierParameters,
          pluginDetailsProvider: PluginDetailsProvider,
          tasks: List<VerifierTask>,
          reportage: VerificationReportage,
          jdkDescriptor: JdkDescriptor): List<Result> {
    val concurrentWorkers = estimateNumberOfConcurrentWorkers(reportage)
    return VerifierExecutor(concurrentWorkers).use {
      it.verify(tasks, jdkDescriptor, verifierParameters, pluginDetailsProvider, reportage)
    }
  }

  private fun estimateNumberOfConcurrentWorkers(reportage: VerificationReportage): Int {
    val availableMemory = Runtime.getRuntime().maxMemory()
    val availableCpu = Runtime.getRuntime().availableProcessors().toLong()
    val maxByMemory = availableMemory / AVERAGE_VERIFIER_MEMORY
    val concurrencyLevel = maxOf(4, minOf(maxByMemory, availableCpu)).toInt()
    reportage.logVerificationExecutorCreated(availableMemory, availableCpu, concurrencyLevel)
    return concurrencyLevel
  }


}