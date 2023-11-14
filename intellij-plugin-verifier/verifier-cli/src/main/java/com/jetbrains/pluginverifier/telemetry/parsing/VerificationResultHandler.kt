package com.jetbrains.pluginverifier.telemetry.parsing

import com.jetbrains.pluginverifier.telemetry.VerificationSpecificTelemetry

interface VerificationResultHandler {
  fun beforeVerificationResult() {}

  fun onVerificationResult(verificationSpecificTelemetry: VerificationSpecificTelemetry) {}

  fun afterVerificationResult() {}
}