package com.jetbrains.pluginverifier.misc

import org.apache.commons.lang3.time.DurationFormatUtils
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.util.concurrent.TimeUnit

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

/**
 * Checks that two values are equal,
 * or throws [IllegalStateException]
 * with a message otherwise.
 */
fun <T> checkEquals(expected: T, actual: T) {
  check(expected == actual) { "Values mismatch:\nExpected: $expected\nActual: $actual" }
}

/**
 * Checks whether the current thread has been interrupted.
 * Clears the *interrupted status* and throws [InterruptedException]
 * if it is the case.
 */
@Throws(InterruptedException::class)
fun checkIfInterrupted() {
  if (Thread.interrupted()) {
    throw InterruptedException()
  }
}

/**
 * Checks whether the current thread has been interrupted.
 * Clears the *interrupted status*, invokes the [action],
 * and throws [InterruptedException] if it is the case.
 */
@Throws(InterruptedException::class)
fun checkIfInterrupted(action: () -> Unit) {
  if (Thread.interrupted()) {
    action()
    throw InterruptedException()
  }
}

fun impossible(): Nothing = throw AssertionError("Impossible")

/**
 * `equals()` for URL that doesn't require internet connection in contrast to [URL.equals]
 */
fun URL.safeEquals(other: URL) = toExternalForm().trimEnd('/') == other.toExternalForm().trimEnd('/')

/**
 * `hashCode()` for URL that doesn't require internet connection in contrast to [URL.hashCode]
 */
fun URL.safeHashCode() = toExternalForm().trim('/').hashCode()

fun <T, R> T.tryInvokeSeveralTimes(attempts: Int,
                                   attemptsDelay: Long,
                                   attemptsDelayTimeUnit: TimeUnit,
                                   presentableBlockName: String,
                                   block: T.() -> R): R {
  for (attempt in 1..attempts) {
    try {
      return block()
    } catch (ie: InterruptedException) {
      throw ie
    } catch (e: Exception) {
      val delayMillis = attemptsDelayTimeUnit.toMillis(attemptsDelay)
      LOG.error("Failed attempt #$attempt of $attempts to invoke '$presentableBlockName'. Wait for $delayMillis millis to reattempt", e)
      Thread.sleep(delayMillis)
    }
  }
  throw RuntimeException("Failed to invoke $presentableBlockName in $attempts attempts")
}