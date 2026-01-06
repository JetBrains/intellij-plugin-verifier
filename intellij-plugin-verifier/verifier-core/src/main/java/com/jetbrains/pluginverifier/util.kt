/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier

fun getConcurrencyLevel(): Int {
  val fromProperty = System.getProperty("intellij.plugin.verifier.concurrency.level")?.toIntOrNull()
  if (fromProperty != null) {
    check(fromProperty > 0) { "Invalid concurrency level: $fromProperty" }
    return fromProperty
  }

  val availableMemory = Runtime.getRuntime().maxMemory()
  val availableCpu = Runtime.getRuntime().availableProcessors().toLong()
  //About 200 Mb is needed for an average verification
  val maxByMemory = availableMemory / 1024 / 1024 / 200
  return maxOf(8, minOf(maxByMemory, availableCpu)).toInt()
}