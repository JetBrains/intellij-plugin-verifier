package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.ClassResolver

interface VerificationContext {
  val classResolver: ClassResolver

  val problemRegistrar: ProblemRegistrar

  val apiUsageProcessors: List<ApiUsageProcessor>
}