package com.jetbrains.pluginverifier.misc

import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.util.concurrent.TimeUnit

private val LOG = LoggerFactory.getLogger("LanguageUtils")

private const val IN_SECOND = 1000
private const val IN_MINUTE = 60 * IN_SECOND
private const val IN_HOUR = 60 * IN_MINUTE
private const val IN_DAY = 24 * IN_HOUR

fun Duration.formatDuration(): String {
  var millis = toMillis()
  val days = millis / IN_DAY
  millis %= IN_DAY

  val hours = millis / IN_HOUR
  millis %= IN_HOUR

  val minutes = millis / IN_MINUTE
  millis %= IN_MINUTE

  val seconds = millis / IN_SECOND
  millis %= IN_SECOND

  if (days > 0) {
    return "$days d $hours h $minutes m"
  }
  if (hours > 0) {
    return "$hours h $minutes m $seconds s"
  }
  if (minutes > 0) {
    return "$minutes m $seconds s $millis ms"
  }
  if (seconds > 0) {
    return "$seconds s $millis ms"
  }
  return "$millis ms"
}

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

fun <T, R> T.retry(
    presentableBlockName: String,
    attempts: Int = 5,
    attemptsDelay: Long = 30,
    attemptsDelayTimeUnit: TimeUnit = TimeUnit.SECONDS,
    block: T.() -> R
): R {
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