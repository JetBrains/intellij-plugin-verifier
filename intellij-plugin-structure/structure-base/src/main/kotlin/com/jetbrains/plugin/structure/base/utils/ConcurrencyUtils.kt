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
      val exception: Throwable?,
      val elapsedTime: Long
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
    val completionService = ExecutorCompletionService<TimedResult<T>>(executor)
    val futures = arrayListOf<Future<TimedResult<T>>>()
    try {
      for (task in tasks) {
        val timedCallable = TimedCallable(task)
        val future = try {
          completionService.submit(timedCallable)
        } catch (e: RejectedExecutionException) {
          if (executor.isShutdown) {
            throw InterruptedException()
          }
          throw RuntimeException("Failed to schedule task", e)
        }
        futures.add(future)
      }
      return waitAllFutures(futures.size, completionService)
    } catch (e: Throwable) {
      for (worker in futures) {
        worker.cancel(true)
      }
      throw e
    }
  }

  private fun waitAllFutures(futuresNumber: Int, completionService: ExecutorCompletionService<TimedResult<T>>): List<T> {
    val results = arrayListOf<T>()
    val exceptions = arrayListOf<Throwable>()
    for (finished in 1..futuresNumber) {
      while (true) {
        checkIfInterrupted()
        val future = completionService.poll(100, TimeUnit.MILLISECONDS)
        if (future != null) {
          val timedResult = try {
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
            throw e.cause!!
          }

          val exception = timedResult.exception
          if (exception != null) {
            if (failFastOnException) {
              throw RuntimeException("Worker finished with error", exception)
            } else {
              exceptions += exception
              progress(ProgressData(finished, futuresNumber, null, exception, timedResult.elapsedTime))
            }
          } else {
            val result = timedResult.result!!
            progress(ProgressData(finished, futuresNumber, result, null, timedResult.elapsedTime))
            results += result
          }
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

  private data class TimedResult<T>(
    val result: T?,
    val exception: Throwable?,
    val elapsedTime: Long
  )

  private class TimedCallable<T>(private val delegate: Callable<T>) : Callable<TimedResult<T>> {
    override fun call(): TimedResult<T> {
      val start = System.currentTimeMillis()
      var result: T? = null
      var exception: Throwable? = null
      try {
        result = delegate.call()
      } catch (e: Throwable) {
        exception = e
      }
      val elapsedTime = System.currentTimeMillis() - start
      return TimedResult(result, exception, elapsedTime)
    }
  }

}