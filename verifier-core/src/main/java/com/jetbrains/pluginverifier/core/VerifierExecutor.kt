package com.jetbrains.pluginverifier.core

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.causedBy
import com.jetbrains.pluginverifier.misc.findCause
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.results.VerificationResult
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
 *
 * The [VerifierExecutor] can be reused for several [verifications] [verify].
 */
class VerifierExecutor(private val concurrentWorkers: Int,
                       private val pluginDetailsCache: PluginDetailsCache,
                       private val jdkDescriptorCache: JdkDescriptorsCache) : Closeable {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(VerifierExecutor::class.java)
  }

  private val executor = Executors.newFixedThreadPool(concurrentWorkers,
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("verifier-%d")
          .build()
  )

  private val completionService = ExecutorCompletionService<VerificationResult>(executor)

  override fun close() {
    executor.shutdownNow()
  }

  /**
   * Runs the [tasks] concurrently on the thread pool allocated for this [VerifierExecutor].
   * The [parameters] configure the verification.
   * The [reportage] is used to save the verification stages and results.
   */
  fun verify(
      tasks: List<VerifierTask>,
      parameters: VerifierParameters,
      reportage: VerificationReportage
  ): List<VerificationResult> {
    val workers = tasks.map { (pluginInfo, jdkPath, ideDescriptor, dependencyFinder) ->
      val pluginVerificationReportage = reportage.createPluginReportage(pluginInfo, ideDescriptor.ideVersion)
      val pluginVerifier = PluginVerifier(
          pluginInfo,
          ideDescriptor,
          dependencyFinder,
          jdkDescriptorCache,
          jdkPath,
          parameters,
          pluginVerificationReportage,
          pluginDetailsCache
      )
      try {
        completionService.submit(pluginVerifier)
      } catch (e: RejectedExecutionException) {
        throw InterruptedException("Verifier executor rejected the next task")
      }
    }
    return waitForAllResults(workers)
  }

  private fun waitForAllResults(workers: List<Future<VerificationResult>>): List<VerificationResult> {
    val results = arrayListOf<VerificationResult>()
    try {
      for (finished in 1..workers.size) {
        while (true) {
          if (Thread.currentThread().isInterrupted) {
            workers.forEach {
              it.cancel(true)
            }
            throw InterruptedException()
          }
          val future = completionService.poll(500, TimeUnit.MILLISECONDS)
          if (future != null) {
            val result = try {
              future.get()
            } catch (e: Throwable) {
              workers.forEach {
                it.cancel(true)
              }
              val interruptedException = e.findCause(InterruptedException::class.java)
              if (interruptedException != null) {
                throw interruptedException
              }
              throw e
            }
            results.add(result)
            break
          }
        }
      }
    } finally {
      for (worker in workers) {
        try {
          /**
           * Force wait for the worker to finish.
           * It's necessary in cases:
           * 1) An exception has been thrown by any worker in `.get()`.
           * It means that the verification is corrupted.
           *
           * 2) The current thread has been interrupted.
           * It means that the verification has been cancelled.
           *
           * In both cases the thrown exception will be propagated after
           * this finally block finishes.
           */
          worker.get()
        } catch (e: Throwable) {
          if (!e.causedBy(InterruptedException::class.java)) {
            LOG.error("Worker $worker finished abruptly", e)
          }
        }
      }
    }
    return results
  }

}