/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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
    val task: Task<T>,
    val finishedNumber: Int,
    val totalNumber: Int,
    val result: T?,
    val exception: Throwable?,
    val elapsedTime: Long
  )

  data class Task<T>(
    val presentableName: String,
    val callable: Callable<T>
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
  fun executeTasks(tasks: List<Task<T>>): List<T> {
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
          throw RuntimeException("Failed to schedule task ${task.presentableName}", e)
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
              throw RuntimeException("Worker '${timedResult.presentableTaskName}' finished with error", exception)
            } else {
              exceptions += exception
              progress(ProgressData(timedResult.task, finished, futuresNumber, null, exception, timedResult.elapsedTime))
            }
          } else {
            val result = timedResult.result!!
            progress(ProgressData(timedResult.task, finished, futuresNumber, result, null, timedResult.elapsedTime))
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
    val task: Task<T>,
    val result: T?,
    val exception: Throwable?,
    val elapsedTime: Long,
    val presentableTaskName: String
  )

  private class TimedCallable<T>(private val task: Task<T>) : Callable<TimedResult<T>> {
    override fun call(): TimedResult<T> {
      val start = System.nanoTime()
      var result: T? = null
      var exception: Throwable? = null
      try {
        result = task.callable.call()
      } catch (e: Throwable) {
        exception = e
      }
      val elapsedTime = System.nanoTime() - start
      return TimedResult(task, result, exception, elapsedTime / 1_000_000, task.presentableName)
    }
  }

}