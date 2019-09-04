package com.jetbrains.plugin.structure.base.utils

import java.io.Closeable
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

class ExecutorWithProgress<T>(
  executorName: String,
  concurrentWorkers: Int,
  private val failFastOnException: Boolean,
  private val progress: (ProgressData<T>) -> Unit
) : Closeable {

  data class ProgressData<T>(
    val finishedNumber: Int,
    val totalNumber: Int,
    val result: T?,
    val exception: Throwable?
  )

  private val nameCounter = AtomicInteger()

  private val executor = Executors.newFixedThreadPool(concurrentWorkers) { r ->
    Thread(r).apply {
      isDaemon = true
      name = executorName + "_" + nameCounter.incrementAndGet().toString()
    }
  }

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
    val exceptions = arrayListOf<Throwable>()
    for (finished in 1..workers.size) {
      while (true) {
        checkIfInterrupted()
        val future = completionService.poll(100, TimeUnit.MILLISECONDS)
        if (future != null) {
          val result = try {
            future.get()
          } catch (e: InterruptedException) {
            throw e
          } catch (e: CancellationException) {
            throw InterruptedException("Worker has been cancelled")
          } catch (e: ExecutionException) {
            val workerException = e.cause!!
            if (workerException is InterruptedException) {
              throw InterruptedException("Worker has been interrupted")
            }

            if (failFastOnException) {
              throw RuntimeException("Worker finished with error", workerException)
            } else {
              exceptions += workerException
              progress(ProgressData(finished, workers.size, null, workerException))
              continue
            }
          }
          progress(ProgressData(finished, workers.size, result, null))
          results.add(result)
          break
        }
      }
    }
    check(exceptions.isEmpty() || !failFastOnException)
    if (exceptions.isNotEmpty()) {
      val error = RuntimeException("Some workers finished with error")
      exceptions.forEach { error.addSuppressed(it) }
      throw error
    }
    return results
  }

}