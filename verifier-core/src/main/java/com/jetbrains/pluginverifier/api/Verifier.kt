package com.jetbrains.pluginverifier.api

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.utils.VerificationWorker
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
class Verifier(val params: VerifierParams) : Closeable {

  companion object {
    private val LOG = LoggerFactory.getLogger(Verifier::class.java)
  }

  private val runtimeResolver = Resolver.createJdkResolver(params.jdkDescriptor.homeDir)

  private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

  private val completionService = ExecutorCompletionService<VerificationResult>(executor)

  private val futures: MutableList<Future<VerificationResult>> = arrayListOf()

  fun verify(pluginDescriptor: PluginDescriptor, ideDescriptor: IdeDescriptor): Future<VerificationResult> {
    val worker = VerificationWorker(pluginDescriptor, ideDescriptor, runtimeResolver, params)
    val future = completionService.submit(worker)
    futures.add(future)
    return future
  }

  override fun close() {
    runtimeResolver.closeLogged()
    executor.shutdownNow()
  }

  fun getVerificationResults(progress: Progress): List<VerificationResult> {
    var verified = 0
    val results = arrayListOf<VerificationResult>()
    val tasks = futures.size
    (1..tasks).forEach fori@ {
      while (true) {
        if (Thread.currentThread().isInterrupted) {
          throw InterruptedException()
        }

        val future = completionService.poll(500, TimeUnit.MILLISECONDS)
        if (future != null) {
          val result = future.get()
          results.add(result)
          progress.setProgress(((++verified).toDouble()) / tasks)
          val resultString = getVerificationResultText(result)
          progress.setText(resultString)
          LOG.info("Worker $verified/$tasks finished. $resultString")
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