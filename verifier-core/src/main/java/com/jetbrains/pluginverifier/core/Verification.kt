package com.jetbrains.pluginverifier.core

import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.VerifierParams
import com.jetbrains.pluginverifier.logging.VerificationLogger
import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginCreator
import org.apache.commons.io.FileUtils

/**
 * @author Sergey Patrikeev
 */
object Verification {

  private val AVERAGE_VERIFIER_MEMORY = 200 * FileUtils.ONE_MB

  fun run(verifierParams: VerifierParams,
          pluginCreator: PluginCreator,
          tasks: List<Pair<PluginCoordinate, IdeDescriptor>>,
          logger: VerificationLogger): List<Result> {
    val concurrentWorkers = estimateNumberOfConcurrentWorkers(logger)
    logger.logEvent("Creating verifier with $concurrentWorkers " + " worker".pluralize(concurrentWorkers))
    logger.tasksNumber = tasks.size
    return VerifierExecutor().verify(tasks, verifierParams, concurrentWorkers, pluginCreator, logger)
  }

  private fun estimateNumberOfConcurrentWorkers(logger: VerificationLogger): Int {
    val availableMemory = Runtime.getRuntime().maxMemory()
    val availableCpu = Runtime.getRuntime().availableProcessors().toLong()
    logger.logEvent("Available memory: ${availableMemory.bytesToMegabytes()} Mb; Available CPU = $availableCpu")
    val maxByMemory = availableMemory / AVERAGE_VERIFIER_MEMORY
    return maxOf(4, minOf(maxByMemory, availableCpu)).toInt()
  }


}