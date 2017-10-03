package com.jetbrains.pluginverifier.core

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.plugin.structure.classes.jdk.JdkResolverCreator
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.VerifierParams
import com.jetbrains.pluginverifier.logging.VerificationLogger
import com.jetbrains.pluginverifier.misc.checkIfInterrupted
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginCreator
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
class VerifierExecutor {

  fun verify(
      tasks: List<Pair<PluginCoordinate, IdeDescriptor>>,
      params: VerifierParams,
      concurrentWorkers: Int,
      pluginCreator: PluginCreator,
      logger: VerificationLogger
  ): List<Result> {
    val executor = Executors.newFixedThreadPool(concurrentWorkers,
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("verifier-%d")
            .build()
    )
    val completionService = ExecutorCompletionService<Result>(executor)

    try {
      val runtimeResolver = JdkResolverCreator.createJdkResolver(params.jdkDescriptor.homeDir)
      return runtimeResolver.use {
        for ((pluginCoordinate, ideDescriptor) in tasks) {
          val pluginLogger = logger.createPluginLogger(pluginCoordinate, ideDescriptor)
          val verifier = Verifier(pluginCoordinate, ideDescriptor, runtimeResolver, params, pluginCreator, pluginLogger)
          completionService.submit(verifier)
        }
        waitForResults(completionService, tasks.size)
      }
    } finally {
      executor.shutdownNow()
    }
  }

  private fun waitForResults(completionService: ExecutorCompletionService<Result>, tasks: Int): List<Result> {
    val results = arrayListOf<Result>()
    (1..tasks).forEach fori@ {
      while (true) {
        checkIfInterrupted()
        val future = completionService.poll(500, TimeUnit.MILLISECONDS)
        if (future != null) {
          val result = future.get()
          results.add(result)
          break
        }
      }
    }
    return results
  }

}