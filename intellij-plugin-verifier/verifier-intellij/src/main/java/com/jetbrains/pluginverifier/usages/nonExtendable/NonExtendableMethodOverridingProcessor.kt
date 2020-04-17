/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.nonExtendable

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class NonExtendableMethodOverridingProcessor(private val nonExtendableApiRegistrar: NonExtendableApiRegistrar) : MethodOverridingProcessor {
  override fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext) {
    if (overriddenMethod.isNonExtendable()) {
      nonExtendableApiRegistrar.registerNonExtendableApiUsage(NonExtendableMethodOverriding(overriddenMethod.location, method.location))
    }
  }
}