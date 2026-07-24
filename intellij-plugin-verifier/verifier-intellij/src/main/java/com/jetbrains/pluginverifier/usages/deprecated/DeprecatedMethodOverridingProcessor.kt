/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.KotlinMethods.isKotlinDefaultMethod
import com.jetbrains.pluginverifier.verifiers.method.MethodOverridingProcessor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class DeprecatedMethodOverridingProcessor(private val deprecatedApiRegistrar: DeprecatedApiRegistrar) : MethodOverridingProcessor {
  override fun processMethodOverriding(method: Method, overriddenMethod: Method, context: VerificationContext) {
    // Kotlin compiles interface default methods by generating, in the implementing class, a stub
    // override that just forwards to the interface's `$DefaultImpls` holder. That stub is
    // bytecode-indistinguishable from a genuine override, so without this guard a deprecated
    // default method would be reported as "overridden" in every implementing class, even though
    // the plugin author never wrote such an override.
    // Note: this only recognizes the pre-`-Xjvm-default` `$DefaultImpls`-forwarding shape.
    if (method.isKotlinDefaultMethod()) return

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