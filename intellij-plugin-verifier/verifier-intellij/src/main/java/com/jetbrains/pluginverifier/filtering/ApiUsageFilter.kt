/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering

import com.jetbrains.pluginverifier.usages.ApiUsage
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * Classifies API usages to reported and ignored. This can be used to filter out
 * unrelated, irrelevant or unnecessary API usages from verification results.
 */
interface ApiUsageFilter {
  fun shouldReport(apiUsage: ApiUsage, context: VerificationContext): Result

  sealed class Result {
    object Report : Result()

    data class Ignore(val reason: String) : Result()
  }
}