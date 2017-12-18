package com.jetbrains.pluginverifier.misc

import org.apache.commons.lang3.time.DurationFormatUtils
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
private val LOG = LoggerFactory.getLogger("LanguageUtils")

fun Duration.formatDuration(format: String): String =
    DurationFormatUtils.formatDuration(toMillis(), format)

fun <T, R> T.doLogged(action: String, block: T.() -> R) {
  try {
    block()
  } catch (e: Exception) {
    LOG.error("Unable to $action", e)
  }
}

fun checkIfInterrupted() {
  if (Thread.currentThread().isInterrupted) {
    throw InterruptedException()
  }
}

fun impossible(): Nothing = throw AssertionError("Impossible")

fun <T, R> T.tryInvokeSeveralTimes(attempts: Int,
                                   attemptsDelay: Long,
                                   attemptsDelayTimeUnit: TimeUnit,
                                   presentableBlockName: String, block: T.() -> R): R {
  for (attempt in 1..attempts) {
    try {
      return block()
    } catch (e: Exception) {
      val delayMillis = attemptsDelayTimeUnit.toMillis(attemptsDelay)
      LOG.error("Failed attempt #$attempt of $attempts to invoke '$presentableBlockName'. Wait for $delayMillis millis to reattempt", e)
      Thread.sleep(delayMillis)
    }
  }
  throw RuntimeException("Failed to invoke $presentableBlockName in $attempts attempts")
}