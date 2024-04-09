 /*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.doesOverride
import org.objectweb.asm.tree.AbstractInsnNode

/**
 * Usage filter for synthetic bridge method generated in specific inheritance scenarios
 * for parameterized classes with generics.
 *
 * @param nonBridgeMethodApiUsageFilterDelegate a delegate [ApiUsageFilter][API Usage filter] that handles non-bridge methods
 *
 * See [Java Generics FAQ](http://www.angelikalanger.com/GenericsFAQ/FAQSections/TechnicalDetails.html#FAQ103)
 */
class BridgeMethodOverrideUsageFilter(private val nonBridgeMethodApiUsageFilterDelegate: ApiUsageFilter) : ApiUsageFilter {
  override fun allowMethodInvocation(invokedMethod: Method,
                                     invocationInstruction: AbstractInsnNode,
                                     callerMethod: Method,
                                     context: VerificationContext): Boolean {
    return if (callerMethod.isBridgeMethod) {
      allowBridgeMethodInvocation(invokedMethod, invocationInstruction, callerMethod, context)
    } else {
      nonBridgeMethodApiUsageFilterDelegate.allowMethodInvocation(
        invokedMethod,
        invocationInstruction,
        callerMethod,
        context)
    }
  }

  //
  private fun allowBridgeMethodInvocation(invokedMethod: Method,
                                          invocationInstruction: AbstractInsnNode,
                                          callerMethod: Method,
                                          context: VerificationContext): Boolean {
    return invokedMethod.doesOverride(callerMethod, context.classResolver)
  }
}