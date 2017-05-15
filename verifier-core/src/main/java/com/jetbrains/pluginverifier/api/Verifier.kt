package com.jetbrains.pluginverifier.api

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.utils.VerificationUtil
import com.jetbrains.pluginverifier.utils.VerificationWorker
import org.slf4j.LoggerFactory
import java.util.concurrent.*

/**
 * @author Sergey Patrikeev
 */
class Verifier(val params: VerifierParams) {

  companion object {
    private val LOG = LoggerFactory.getLogger(Verifier::class.java)
  }

  fun verify(progress: Progress = DefaultProgress()): List<Result> {
    val startMessage = "Verification of " + "plugin".pluralize(params.pluginsToCheck.size) + " is starting"
    LOG.debug(startMessage)

    val startTime = System.currentTimeMillis()
    progress.setText(startMessage)
    progress.setProgress(0.0)
    try {
      return runVerifierUnderProgress(progress)
    } finally {
      val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
      LOG.debug("The verification has been successfully completed in $elapsedSeconds seconds")
      progress.setText("The verification is finished in $elapsedSeconds seconds")
      progress.setProgress(1.0)
    }
  }

  private fun runVerifierUnderProgress(progress: Progress): List<Result> {
    val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    val completionService = ExecutorCompletionService<Result>(executor)

    val results = arrayListOf<Result>()
    try {
      createJdkResolver().use { runtimeResolver ->
        val ideToPlugins = params.pluginsToCheck.groupBy({ it.second }, { it.first }).entries

        ideToPlugins.forEach { ideToPlugins ->
          val ideDescriptor = ideToPlugins.key

          VerificationUtil.createIdeAndResolver(ideDescriptor).use { (ide, ideResolver) ->
            val futures = ideToPlugins.value.map { pluginDescriptor ->
              val worker = VerificationWorker(pluginDescriptor, ide, ideResolver, runtimeResolver, params)
              completionService.submit(worker)
            }

            results.addAll(waitForWorkersCompletion(executor, completionService, ideDescriptor, progress, futures))
          }
        }
      }
    } finally {
      executor.shutdownNow()
    }
    require(results.size == params.pluginsToCheck.size)
    return results
  }

  private fun createJdkResolver() = Resolver.createJdkResolver(params.jdkDescriptor.file)

  private fun waitForWorkersCompletion(executor: ExecutorService,
                                       completionService: ExecutorCompletionService<Result>,
                                       ideDescriptor: IdeDescriptor,
                                       progress: Progress,
                                       futures: List<Future<Result>>): List<Result> {
    var verified = 0
    val results = arrayListOf<Result>()
    val workers = futures.size
    (1..workers).forEach fori@ {
      while (true) {
        if (Thread.currentThread().isInterrupted) {
          executor.shutdownNow()
          throw InterruptedException()
        }

        val future = completionService.poll(500, TimeUnit.MILLISECONDS)
        if (future != null) {
          val result = future.get()
          results.add(result)
          progress.setProgress(((++verified).toDouble()) / workers)
          val statusString = "${result.plugin} has been verified with $ideDescriptor. Result: $result"
          progress.setText(statusString)
          LOG.trace("$statusString; Finished $verified out of $workers workers")
          break
        }
      }
    }
    return results
  }

}