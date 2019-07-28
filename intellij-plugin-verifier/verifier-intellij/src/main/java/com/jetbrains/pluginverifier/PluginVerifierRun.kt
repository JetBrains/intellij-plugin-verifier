package com.jetbrains.pluginverifier

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.plugin.structure.base.utils.checkIfInterrupted
import com.jetbrains.plugin.structure.base.utils.shutdownAndAwaitTermination
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.reporting.PluginReporters
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import java.io.Closeable
import java.util.concurrent.*

fun runSeveralVerifiers(reportage: PluginVerificationReportage, verifiers: List<PluginVerifier>): List<PluginVerificationResult> {
  if (verifiers.isEmpty()) {
    return emptyList()
  }

  val executor = ExecutorWithProgress<PluginVerificationResult>(getConcurrencyLevel()) { progressData ->
    val result = progressData.result
    reportage.logVerificationStage(
        "Finished ${progressData.finishedNumber} of ${progressData.totalNumber} verifications: " +
            "${result.verificationTarget} against ${result.plugin}: ${result.verificationVerdict}"
    )
  }

  val tasks = verifiers.map { verifier ->
    Callable {
      reportage.createPluginReporters(verifier.plugin, verifier.verificationTarget).use { reporters ->
        val verificationResult = verifier.loadPluginAndVerify()
        reporters.reportResults(verificationResult)
        verificationResult
      }
    }
  }
  return executor.executeTasks(tasks)
}

private fun getConcurrencyLevel(): Int {
  val fromProperty = System.getProperty("intellij.plugin.verifier.concurrency.level")?.toIntOrNull()
  if (fromProperty != null) {
    check(fromProperty > 0) { "Invalid concurrency level: $fromProperty" }
    return fromProperty
  }

  val availableMemory = Runtime.getRuntime().maxMemory()
  val availableCpu = Runtime.getRuntime().availableProcessors().toLong()
  //About 200 Mb is needed for an average verification
  val maxByMemory = availableMemory / 1024 / 1024 / 200
  return maxOf(4, minOf(maxByMemory, availableCpu)).toInt()
}

private fun PluginReporters.reportResults(result: PluginVerificationResult) {
  reportVerificationResult(result)

  if (result is PluginVerificationResult.Verified) {
    result.compatibilityWarnings.forEach { reportNewWarningDetected(it) }
    result.compatibilityProblems.forEach { reportNewProblemDetected(it) }
    result.deprecatedUsages.forEach { reportDeprecatedUsage(it) }
    result.experimentalApiUsages.forEach { reportExperimentalApi(it) }
    reportDependencyGraph(result.dependenciesGraph)

    for ((problem, reason) in result.ignoredProblems) {
      reportProblemIgnored(
          ProblemIgnoredEvent(
              result.plugin,
              result.verificationTarget,
              problem,
              reason
          )
      )
    }
  }
}

private class ExecutorWithProgress<T>(
    concurrentWorkers: Int,
    private val progress: (ProgressData<T>) -> Unit
) : Closeable {

  data class ProgressData<T>(val finishedNumber: Int, val totalNumber: Int, val result: T)

  private val executor = Executors.newFixedThreadPool(
      concurrentWorkers,
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("executor-%d")
          .build()
  )

  override fun close() {
    executor.shutdownAndAwaitTermination(1, TimeUnit.MINUTES)
  }

  @Throws(InterruptedException::class)
  fun executeTasks(tasks: List<Callable<T>>): List<T> {
    val completionService = ExecutorCompletionService<T>(executor)
    val workers = arrayListOf<Future<T>>()
    try {
      for (task in tasks) {
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
      return waitAllWorkersWithInterruptionChecks(completionService, workers)
    } catch (e: Throwable) {
      for (worker in workers) {
        worker.cancel(true)
      }
      throw e
    }
  }

  private fun waitAllWorkersWithInterruptionChecks(
      completionService: ExecutorCompletionService<T>,
      workers: List<Future<T>>
  ): List<T> {
    val results = arrayListOf<T>()
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
          progress(ProgressData(finished, workers.size, result))
          results.add(result)
          break
        }
      }
    }
    return results
  }

}