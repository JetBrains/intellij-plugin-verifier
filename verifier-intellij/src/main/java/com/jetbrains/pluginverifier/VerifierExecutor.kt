package com.jetbrains.pluginverifier

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.plugin.structure.base.utils.checkIfInterrupted
import com.jetbrains.pluginverifier.misc.shutdownAndAwaitTermination
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.results.VerificationResult
import java.io.Closeable
import java.util.concurrent.*

/**
 * [Runs] [verify] the verification tasks with a concurrency level of [concurrentWorkers].
 *
 * The [VerifierExecutor] can be reused for several verifications.
 */
class VerifierExecutor(private val concurrentWorkers: Int, private val reportage: Reportage) : Closeable {

  private val executor = Executors.newFixedThreadPool(
      concurrentWorkers,
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("verifier-%d")
          .build()
  )

  override fun close() {
    executor.shutdownAndAwaitTermination(1, TimeUnit.MINUTES)
  }

  /**
   * Runs [tasks] concurrently on the thread pool allocated
   * for this [VerifierExecutor] and returns their results.
   *
   * @throws InterruptedException if the current thread,
   * or any executing worker has been interrupted while waiting.
   */
  @Throws(InterruptedException::class)
  fun verify(tasks: List<PluginVerifier>): List<VerificationResult> {
    val completionService = ExecutorCompletionService<VerificationResult>(executor)
    val workers = arrayListOf<Future<VerificationResult>>()
    try {
      for (task in sortVerificationTasks(tasks)) {
        val worker = try {
          completionService.submit(task)
        } catch (e: RejectedExecutionException) {
          if (executor.isShutdown) {
            throw InterruptedException()
          }
          throw RuntimeException("Failed to schedule task", e)
        }
        workers.add(worker)
      }
      return waitAllWorkersInterruptibly(completionService, workers)
    } catch (e: Throwable) {
      for (worker in workers) {
        worker.cancel(true)
      }
      throw e
    }
  }

  /**
   * Sort verification tasks to increase chances that two verifications of the same plugin
   * would be executed shortly, and therefore caches, such as plugin details cache, would be warmed-up.
   */
  private fun sortVerificationTasks(tasks: List<PluginVerifier>) =
      tasks.sortedBy { it.plugin.pluginId }

  /**
   * Waits for all [workers] to complete.
   *
   * No worker can throw [ExecutionException] with cause other
   * than InterruptedException.
   * It is a fatal error otherwise.
   *
   * Throws [InterruptedException] if:
   * - The current thread has been interrupted while waiting.
   * - Any worker has been cancelled or interrupted
   */
  @Throws(InterruptedException::class)
  private fun waitAllWorkersInterruptibly(
      completionService: ExecutorCompletionService<VerificationResult>,
      workers: List<Future<VerificationResult>>
  ): List<VerificationResult> {
    val results = arrayListOf<VerificationResult>()
    for (finished in 1..workers.size) {
      while (true) {
        checkIfInterrupted()
        val future = completionService.poll(500, TimeUnit.MILLISECONDS) //throws InterruptedException
        if (future != null) {
          val result = try {
            future.get() //propagate InterruptedException
          } catch (e: CancellationException) {
            throw InterruptedException("Worker has been cancelled")
          } catch (e: ExecutionException) {
            if (e.cause is InterruptedException) {
              throw InterruptedException("Worker has been interrupted")
            }
            //Fatal error because no worker can throw exceptions other than InterruptedException
            throw RuntimeException("Fatal: worker finished abruptly", e.cause)
          }
          reportage.logVerificationStage("Finished $finished of ${workers.size} verifications: ${result.verificationTarget} against ${result.plugin}: ${result.verificationVerdict}")
          results.add(result)
          break
        }
      }
    }
    return results
  }

}