/*
 * Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.misc

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.min


private val LOG: Logger = LoggerFactory.getLogger(Retry::class.java)
private val INITIAL_DELAY_MS = TimeUnit.SECONDS.toMillis(10)

fun <T, R> T.retry(presentableBlockName: String, recover: RecoverStrategy = ExponentialBackOff(), body: T.() -> R) = Retry(presentableBlockName = presentableBlockName, maxAttempts = 5, recover = recover, obj = this).retry(body)
fun <T, R> T.poll(presentableBlockName: String, maxAttempts: Int = 10, operation: T.() -> R): R = Retry(presentableBlockName = presentableBlockName, maxAttempts = maxAttempts, recover = Sleep.THIRTY_SECONDS, obj = this).retry(operation)

private class Retry<T>(val presentableBlockName: String, val maxAttempts: Int, val recover: RecoverStrategy, val obj: T) {
  fun <R> retry(body: T.() -> R): R {
    for (attempt in 1..maxAttempts) {
      try {
        return body(obj)
      } catch (e: Throwable) {
        e.rethrowIfInterrupted()
        LOG.warn(
          "Retry #{} of {} to invoke '{}' failed with '{}'",
          attempt, maxAttempts, presentableBlockName, e.message ?: e::class.java
        )
        if (attempt == maxAttempts) {
          val error = "Failed to invoke '$presentableBlockName' in all $maxAttempts attempts, see nested exception for details"
          throw RuntimeException(error, e)
        }
        recover.recover(e, attempt)
      }
    }
    error("Should not be reached")
  }
}

abstract class RecoverStrategy {
  abstract fun recover(error: Throwable, attemptNumber: Int)
}

class Sleep(private val delay: Long, private val unit: TimeUnit = TimeUnit.SECONDS) : RecoverStrategy() {
  companion object {
    val THIRTY_SECONDS = Sleep(30)
  }

  override fun recover(error: Throwable, attemptNumber: Int) {
    LOG.info("Retrying in {} {}", delay, unit)
    unit.sleep(delay)
  }
}

class ExponentialBackOff(initialDelayMs: Long = INITIAL_DELAY_MS) : RecoverStrategy() {
  private companion object {
    val BACKOFF_LIMIT_MS = TimeUnit.MINUTES.toMillis(3)
    const val BACKOFF_FACTOR = 2
    const val BACKOFF_JITTER = 0.1
  }

  private val random by lazy(::Random)
  private var effectiveDelay = initialDelayMs

  override fun recover(error: Throwable, attemptNumber: Int) {
    if (attemptNumber > 1) {
      effectiveDelay = backOff(effectiveDelay, error)
    }
    LOG.info("Retrying in ${effectiveDelay}ms")
    if (effectiveDelay > 0) {
      Thread.sleep(effectiveDelay)
    }
  }

  private fun backOff(previousDelay: Long, cause: Throwable): Long {
    val nextDelay = min(previousDelay * BACKOFF_FACTOR, BACKOFF_LIMIT_MS) +
      (random.nextGaussian() * previousDelay * BACKOFF_JITTER).toLong()
    if (nextDelay > BACKOFF_LIMIT_MS) {
      throw Exception("Back off limit ${BACKOFF_LIMIT_MS}ms exceeded, see nested exception for details", cause)
    }
    return nextDelay
  }
}