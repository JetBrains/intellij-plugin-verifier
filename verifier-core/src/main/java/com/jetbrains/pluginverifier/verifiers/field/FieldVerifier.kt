package com.jetbrains.pluginverifier.verifiers.field

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.Field

interface FieldVerifier {
  fun verify(field: Field, context: VerificationContext)
}
