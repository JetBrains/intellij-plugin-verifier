package com.jetbrains.pluginverifier

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.causedBy
import com.jetbrains.pluginverifier.misc.findCause
import com.jetbrains.pluginverifier.results.VerificationResult
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.*

/**
 * Verification executor that [runs] [verify] the verification tasks
 * with a concurrency level of [concurrentWorkers].
 *
 * The [VerifierExecutor] can be reused for several verifications.
 */
class VerifierExecutor(private val concurrentWorkers: Int) : Closeable {

  companion object {
    private val LOG = LoggerFactory.getLogger(VerifierExecutor::class.java)
  }

  private val executor = Executors.newFixedThreadPool(concurrentWorkers,
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("verifier-%d")
          .build()
  )

  override fun close() {
    executor.shutdownNow()
  }

  /**
   * Runs the [tasks] concurrently on the thread pool allocated for this [VerifierExecutor].
   * The [parameters] configure the verification.
   * The [reportage] is used to save the verification stages and results.
   */
  fun verify(tasks: List<PluginVerifier>): List<VerificationResult> {
    val completionService = ExecutorCompletionService<VerificationResult>(executor)
    val workers = try {
      tasks.map { completionService.submit(it) }
    } catch (e: RejectedExecutionException) {
      throw InterruptedException("The verifier executor rejected to execute the next task")
    }
    return waitForAllWorkers(completionService, workers)
  }

  private fun waitForAllWorkers(completionService: ExecutorCompletionService<VerificationResult>,
                                workers: List<Future<VerificationResult>>): List<VerificationResult> {
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
           * Force wait for the worker to finish, which is necessary in cases:
           * 1) An exception has been thrown by any worker in `.get()`.
           * It means that the program is corrupted.
           *
           * 2) The current thread has been interrupted.
           * It means that the process has been cancelled.
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