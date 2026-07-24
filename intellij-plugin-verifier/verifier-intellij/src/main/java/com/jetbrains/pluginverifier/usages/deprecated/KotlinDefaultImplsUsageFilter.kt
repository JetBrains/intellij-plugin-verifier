/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

/**
 * Kotlin interface default methods may be implemented via a generated inner class `DefaultImpls`
 * (depending on `-Xjvm-default` mode). Invocations of such synthetic methods are compiler
 * artifacts, not references written by the plugin author, so they should not be reported as
 * deprecated API usages.
 */
class KotlinDefaultImplsUsageFilter : ApiUsageFilter {
  override fun allow(invokedMethod: Method, invocationInstruction: AbstractInsnNode, callerMethod: Method, context: VerificationContext): Boolean =
    invokedMethod.containingClassFile.name.endsWith("\$DefaultImpls")
}
