package com.jetbrains.pluginverifier.core

import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.cleanup.SpaceUnit
import com.jetbrains.pluginverifier.repository.cleanup.bytesToSpaceAmount
import com.jetbrains.pluginverifier.results.VerificationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The main verification entry point.
 */
object Verification {

  private val LOG: Logger = LoggerFactory.getLogger(Verification::class.java)

  private val AVERAGE_VERIFIER_MEMORY = 200

  /**
   * Runs the [verification tasks] [VerifierTask]
   * with a best available concurrency level
   * and returns the [verification results] [VerificationResult].
   */
  fun run(verifierParameters: VerifierParameters,
          pluginDetailsCache: PluginDetailsCache,
          tasks: List<VerifierTask>,
          reportage: VerificationReportage,
          jdkDescriptorsCache: JdkDescriptorsCache
  ): List<VerificationResult> =
      createVerifierExecutor(pluginDetailsCache, jdkDescriptorsCache).use {
        it.verify(tasks, verifierParameters, reportage)
      }

  /**
   * Creates a [VerifierExecutor] with a best available concurrency level.
   */
  fun createVerifierExecutor(
      pluginDetailsCache: PluginDetailsCache,
      jdkDescriptorsCache: JdkDescriptorsCache
  ) = VerifierExecutor(estimateNumberOfConcurrentWorkers(), pluginDetailsCache, jdkDescriptorsCache)

  private fun estimateNumberOfConcurrentWorkers(): Int {
    val availableMemory = Runtime.getRuntime().maxMemory().bytesToSpaceAmount()
    val availableCpu = Runtime.getRuntime().availableProcessors().toLong()
    val maxByMemory = availableMemory.to(SpaceUnit.MEGA_BYTE).toLong() / AVERAGE_VERIFIER_MEMORY
    val concurrencyLevel = maxOf(4, minOf(maxByMemory, availableCpu)).toInt()
    LOG.info("Available memory: $availableMemory; Available CPU = $availableCpu; Concurrency level = $concurrencyLevel")
    return concurrencyLevel
  }


}