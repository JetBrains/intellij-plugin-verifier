/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier

import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

/**
 * Context for [com.jetbrains.pluginverifier.runSeveralVerifiers]
 */
class PluginVerifierBatchContext {
  val deduplicationMap: MutableMap<Any, Any> = ConcurrentHashMap()

  @Suppress("UNCHECKED_CAST", "unused")
  fun <T : Any> deduplicate(t: T): T = deduplicationMap.computeIfAbsent(t, Function.identity()) as T
}
