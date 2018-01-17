package com.jetbrains.pluginverifier.core

import com.jetbrains.pluginverifier.core.Verification.run
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.cleanup.SpaceUnit
import com.jetbrains.pluginverifier.repository.cleanup.bytesToSpaceAmount
import com.jetbrains.pluginverifier.results.VerificationResult

/**
 * The main verification entry point that allows
 * to [run] the [verification tasks] [VerifierTask]
 * with a best available concurrency level.
 */
object Verification {

  private val AVERAGE_VERIFIER_MEMORY = 200

  fun run(verifierParameters: VerifierParameters,
          pluginDetailsCache: PluginDetailsCache,
          tasks: List<VerifierTask>,
          reportage: VerificationReportage,
          jdkDescriptorsCache: JdkDescriptorsCache): List<VerificationResult> {
    val concurrentWorkers = estimateNumberOfConcurrentWorkers(reportage)
    return with(jdkDescriptorsCache.getJdkResolver(verifierParameters.jdkPath)) {
      when (this) {
        is ResourceCacheEntryResult.Found -> {
          resourceCacheEntry.use {
            val jdkDescriptor = resourceCacheEntry.resource
            VerifierExecutor(concurrentWorkers, pluginDetailsCache, jdkDescriptor).use {
              it.verify(tasks, verifierParameters, reportage)
            }
          }
        }
        is ResourceCacheEntryResult.Failed -> throw error
        is ResourceCacheEntryResult.NotFound -> throw IllegalArgumentException("JDK ${verifierParameters.jdkPath} is not found")
      }
    }
  }

  private fun estimateNumberOfConcurrentWorkers(reportage: VerificationReportage): Int {
    val availableMemory = Runtime.getRuntime().maxMemory().bytesToSpaceAmount()
    val availableCpu = Runtime.getRuntime().availableProcessors().toLong()
    val maxByMemory = availableMemory.to(SpaceUnit.MEGA_BYTE).toLong() / AVERAGE_VERIFIER_MEMORY
    val concurrencyLevel = maxOf(4, minOf(maxByMemory, availableCpu)).toInt()
    reportage.logVerificationStage("Available memory: $availableMemory; Available CPU = $availableCpu; Concurrency level = $concurrencyLevel")
    return concurrencyLevel
  }


}