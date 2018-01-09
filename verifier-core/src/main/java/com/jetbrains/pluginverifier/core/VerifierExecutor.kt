package com.jetbrains.pluginverifier.core

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.findCause
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.results.Result
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.*

/**
 * Verification executor that [runs] [verify] the [verification tasks] [VerifierTask]
 * with a concurrency level of [concurrentWorkers].
 *
 * The [pluginDetailsCache] provides the
 * [plugin details] [com.jetbrains.pluginverifier.plugin.PluginDetails]
 * of the verified and dependent plugins.
 */
class VerifierExecutor(private val concurrentWorkers: Int, private val pluginDetailsCache: PluginDetailsCache) : Closeable {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(VerifierExecutor::class.java)
  }

  private val executor: ExecutorService = Executors.newFixedThreadPool(concurrentWorkers,
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("verifier-%d")
          .build()
  )

  private val completionService = ExecutorCompletionService<Result>(executor)

  override fun close() {
    executor.shutdownNow()
  }

  /**
   * Runs the [tasks] concurrently on the thread pool allocated for this [VerifierExecutor].
   * The [jdkDescriptor] is used to resolve the JDK classes.
   * The [parameters] configure the verification.
   * The [reportage] is used to save the verification stages and result.
   */
  fun verify(
      tasks: List<VerifierTask>,
      jdkDescriptor: JdkDescriptor,
      parameters: VerifierParameters,
      reportage: VerificationReportage
  ) = runVerificationConcurrently(tasks, parameters, jdkDescriptor, reportage)

  private fun runVerificationConcurrently(
      tasks: List<VerifierTask>,
      parameters: VerifierParameters,
      jdkDescriptor: JdkDescriptor,
      reportage: VerificationReportage
  ): List<Result> {
    val workers = tasks.map { (pluginInfo, ideDescriptor, dependencyFinder) ->
      val pluginVerificationReportage = reportage.createPluginReportage(pluginInfo, ideDescriptor)
      val verifier = PluginVerifier(pluginInfo, ideDescriptor, dependencyFinder, jdkDescriptor, parameters, pluginVerificationReportage, pluginDetailsCache)
      completionService.submit(verifier)
    }
    return waitForAllResults(workers)
  }

  private fun waitForAllResults(workers: List<Future<Result>>): List<Result> {
    val results = arrayListOf<Result>()
    try {
      for (finished in 1..workers.size) {
        while (true) {
          if (Thread.currentThread().isInterrupted) {
            for (worker in workers) {
              worker.cancel(true)
            }
            throw InterruptedException()
          }
          val future = completionService.poll(500, TimeUnit.MILLISECONDS)
          if (future != null) {
            val result = future.get()
            results.add(result)
            break
          }
        }
      }
    } finally {
      for (worker in workers) {
        try {
          //Force wait for the worker to finish.
          //It is necessary in case the current thread has been interrupted
          //and the interruption has sent to all the workers.
          worker.get()
        } catch (e: Throwable) {
          val interruptedException = e.findCause(InterruptedException::class.java)
          if (interruptedException == null) {
            LOG.error("Worker $worker finished abruptly", e)
          }
        }
      }
    }
    return results
  }

}