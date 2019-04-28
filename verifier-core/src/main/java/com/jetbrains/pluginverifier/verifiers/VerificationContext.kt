package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.verifiers.resolution.ClassResolver

interface VerificationContext {
  val classResolver: ClassResolver

  val problemRegistrar: ProblemRegistrar

  val deprecatedApiRegistrar: DeprecatedApiRegistrar

  val experimentalApiRegistrar: ExperimentalApiRegistrar
}