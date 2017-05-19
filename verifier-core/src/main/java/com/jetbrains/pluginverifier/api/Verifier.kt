package com.jetbrains.pluginverifier.api

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.utils.VerificationWorker
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
class Verifier(val params: VerifierParams) {

  companion object {
    private val LOG = LoggerFactory.getLogger(Verifier::class.java)
  }

  fun verify(progress: Progress = DefaultProgress()): List<VerificationResult> {
    val startMessage = "Verification of ${params.pluginsToCheck.size} " + "plugin".pluralize(params.pluginsToCheck.size) + " is starting"
    LOG.info(startMessage)

    val startTime = System.currentTimeMillis()
    progress.setText(startMessage)
    progress.setProgress(0.0)
    try {
      return runVerifierUnderProgress(progress)
    } finally {
      val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
      val finishMessage = "Verification finished in $elapsedSeconds seconds"
      LOG.info(finishMessage)
      progress.setText(finishMessage)
      progress.setProgress(1.0)
    }
  }

  private fun runVerifierUnderProgress(progress: Progress): List<VerificationResult> {
    val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    val completionService = ExecutorCompletionService<VerificationResult>(executor)

    val results = arrayListOf<VerificationResult>()
    try {
      createJdkResolver().use { runtimeResolver ->
        val ideToPlugins = params.pluginsToCheck.groupBy({ it.second }, { it.first }).entries

        ideToPlugins.forEach { (ideDescriptor, plugins) ->
          val futures = plugins.map { pluginDescriptor ->
            val worker = VerificationWorker(pluginDescriptor, ideDescriptor, runtimeResolver, params)
            completionService.submit(worker)
          }
          results.addAll(waitForWorkersCompletion(completionService, progress, futures))
        }
      }
    } finally {
      executor.shutdownNow()
    }
    require(results.size == params.pluginsToCheck.size)
    return results
  }

  private fun createJdkResolver() = Resolver.createJdkResolver(params.jdkDescriptor.homeDir)

  private fun waitForWorkersCompletion(completionService: ExecutorCompletionService<VerificationResult>,
                                       progress: Progress,
                                       futures: List<Future<VerificationResult>>): List<VerificationResult> {
    var verified = 0
    val results = arrayListOf<VerificationResult>()
    val workers = futures.size
    (1..workers).forEach fori@ {
      while (true) {
        if (Thread.currentThread().isInterrupted) {
          throw InterruptedException()
        }

        val future = completionService.poll(500, TimeUnit.MILLISECONDS)
        if (future != null) {
          val result = future.get()
          results.add(result)
          progress.setProgress(((++verified).toDouble()) / workers)
          val resultString = getVerificationResultText(result)
          progress.setText(resultString)
          LOG.info("Worker $verified/$workers finished. $resultString")
          break
        }
      }
    }
    return results
  }

  private fun getVerificationResultText(result: VerificationResult): String =
      "Plugin ${result.pluginDescriptor} has been verified with ${result.ideDescriptor}. " + when (result) {
        is VerificationResult.Verified -> "Verified: ${result.verdict}"
        is VerificationResult.BadPlugin -> "Broken plugin: ${result.problems.joinToString()}"
        is VerificationResult.NotFound -> "Not found: ${result.reason}"
      }

}