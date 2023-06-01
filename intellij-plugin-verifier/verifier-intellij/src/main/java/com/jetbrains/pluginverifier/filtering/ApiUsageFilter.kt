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