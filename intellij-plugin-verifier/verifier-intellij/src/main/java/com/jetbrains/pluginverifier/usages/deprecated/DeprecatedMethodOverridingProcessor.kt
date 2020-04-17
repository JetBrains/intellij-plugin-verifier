/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class DeprecatedMethodOverridingProcessor(private val deprecatedApiRegistrar: DeprecatedApiRegistrar) : MethodOverridingProcessor {
  override fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext) {
    val methodDeprecated = overriddenMethod.deprecationInfo
    if (methodDeprecated != null) {
      deprecatedApiRegistrar.registerDeprecatedUsage(
        DeprecatedMethodOverridden(
          overriddenMethod.location,
          method.location,
          methodDeprecated
        )
      )
    }
  }
}