/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class ExperimentalMethodOverridingProcessor(private val experimentalApiRegistrar: ExperimentalApiRegistrar) : MethodOverridingProcessor {
  override fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext) {
    if (overriddenMethod.isExperimentalApi(context.classResolver, usageLocation = method.location)) {
      experimentalApiRegistrar.registerExperimentalApiUsage(
        ExperimentalMethodOverridden(
          overriddenMethod.location,
          method.location
        )
      )
    }
  }
}