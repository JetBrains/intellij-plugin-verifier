package com.jetbrains.pluginverifier.core

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.plugin.structure.classes.jdk.JdkResolverCreator
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.logging.VerificationLogger
import com.jetbrains.pluginverifier.misc.checkIfInterrupted
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.results.Result
import java.io.Closeable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
class VerifierExecutor(concurrentWorkers: Int) : Closeable {

  private val executor: ExecutorService = Executors.newFixedThreadPool(concurrentWorkers,
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("verifier-%d")
          .build()
  )

  override fun close() {
    executor.shutdownNow()
  }

  fun verify(
      tasks: List<Pair<PluginCoordinate, IdeDescriptor>>,
      parameters: VerifierParameters,
      pluginDetailsProvider: PluginDetailsProvider,
      logger: VerificationLogger
  ): List<Result> {
    val jdkResolver = JdkResolverCreator.createJdkResolver(parameters.jdkDescriptor.homeDir)
    return jdkResolver.use {
      runVerificationConcurrently(executor, tasks, logger, jdkResolver, parameters, pluginDetailsProvider)
    }
  }

  private fun runVerificationConcurrently(
      executor: ExecutorService,
      tasks: List<Pair<PluginCoordinate, IdeDescriptor>>,
      logger: VerificationLogger,
      jdkResolver: Resolver,
      parameters: VerifierParameters,
      pluginDetailsProvider: PluginDetailsProvider
  ): List<Result> {
    val completionService = ExecutorCompletionService<Result>(executor)
    for ((pluginCoordinate, ideDescriptor) in tasks) {
      val pluginLogger = logger.createPluginLogger(pluginCoordinate, ideDescriptor)
      val verifier = Verifier(pluginCoordinate, ideDescriptor, jdkResolver, parameters, pluginDetailsProvider, pluginLogger)
      completionService.submit(verifier)
    }
    return waitForResults(completionService, tasks.size)
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