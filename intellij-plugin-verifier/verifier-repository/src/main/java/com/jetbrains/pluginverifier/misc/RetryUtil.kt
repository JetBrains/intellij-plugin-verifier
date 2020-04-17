/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.misc

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val LOG = LoggerFactory.getLogger("LanguageUtils")

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
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      val delayMillis = attemptsDelayTimeUnit.toMillis(attemptsDelay)
      LOG.error("Failed attempt #$attempt of $attempts to invoke '$presentableBlockName'. Wait for $delayMillis millis to reattempt", e)
      Thread.sleep(delayMillis)
    }
  }
  throw RuntimeException("Failed to invoke $presentableBlockName in $attempts attempts")
}