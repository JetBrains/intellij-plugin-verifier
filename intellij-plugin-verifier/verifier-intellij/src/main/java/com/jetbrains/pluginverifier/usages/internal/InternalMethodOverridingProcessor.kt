/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.hasDifferentOrigin

class InternalMethodOverridingProcessor(private val internalApiUsageRegistrar: InternalApiUsageRegistrar) : MethodOverridingProcessor {
  override fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext) {
    if (overriddenMethod.isInternalApi(context.classResolver, method.location)
      && overriddenMethod.hasDifferentOrigin(method)) {
      // As of Kotlin 2.2, the Kotlin compiler generates a stub method on interface implementations
      // that only calls the corresponding superclass/interface's method. These stub methods are not
      // really differentiable from when a user would manually do such an override, so we can get
      // false positives in this case.
      internalApiUsageRegistrar.registerInternalApiUsage(
        InternalMethodOverridden(
          overriddenMethod.location,
          method.location
        )
      )
    }
  }
}