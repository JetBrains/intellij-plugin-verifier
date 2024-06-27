/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.usages.util.findEffectiveMemberAnnotation
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.CompositeApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.filter.SameModuleUsageFilter
import com.jetbrains.pluginverifier.verifiers.hasAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.searchParentOverrides
import org.objectweb.asm.tree.AbstractInsnNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(OverrideOnlyMethodUsageProcessor::class.java)

class OverrideOnlyMethodUsageProcessor(private val overrideOnlyRegistrar: OverrideOnlyRegistrar) : ApiUsageProcessor {

  private val allowedOverrideOnlyUsageFilter = CompositeApiUsageFilter(
    SameModuleUsageFilter(),
    CallOfSuperConstructorOverrideOnlyAllowedUsageFilter(),
    DelegateCallOnOverrideOnlyUsageFilter().withBridgeMethodSupport(),
    SuperclassCallOnOverrideOnlyUsageFilter()
  )

  override fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    instructionNode: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) {


    if (resolvedMethod.isOverrideOnlyMethod(context)
      && !isAllowedOverrideOnlyUsage(methodReference, resolvedMethod, instructionNode, callerMethod, context))
    {
      overrideOnlyRegistrar.registerOverrideOnlyMethodUsage(
        OverrideOnlyMethodUsage(methodReference, resolvedMethod.location, callerMethod.location)
      )
    }
  }

  /**
   * Detects if this is an allowed method invocation.
   * In other words, if this is an allowed scenario where the invocation of such method is allowed and will not
   * be reported as a plugin problem.
   *
   * Example:
   * ```
   *   public void usages(OverrideOnlyMethodOwner owner1,) {
   *     owner1.overrideOnlyMethod();
   *   }
   * ```
   * Description:
   * - Both _Invoked Method Reference_ and _Invoked Method_ will refer to the `overrideOnlyMethod`.
   * - _Invocation Instruction_ will refer to the JVM bytecode instruction which will invoke the `overrideOnlyMethod`.
   * In this sample, this will be `182/invokevirtual` opcode.
   * - _Caller Method_ refers to the `usages()` method
   * @param invokedMethodReference a reference to the `OverrideOnly` method that is being invoked
   * @param invokedMethod the `OverrideOnly` method that is being invoked
   * @param invocationInstruction low-level JVM invocation instruction used to invoke the `OverrideOnly` method
   * @param callerMethod the method in which the invocation of `OverrideOnly` method occurs.
   * @param context the verification context with additional metadata and data
   *
   */
  private fun isAllowedOverrideOnlyUsage(invokedMethodReference: MethodReference,
                                         invokedMethod: Method,
                                         invocationInstruction: AbstractInsnNode,
                                         callerMethod: Method,
                                         context: VerificationContext): Boolean {
    return allowedOverrideOnlyUsageFilter.allowMethodInvocation(invokedMethod, invocationInstruction, callerMethod, context)
  }

  private fun Method.isOverrideOnlyMethod(context: VerificationContext): Boolean =
    annotations.hasAnnotation(overrideOnlyAnnotationName)
      || containingClassFile.annotations.hasAnnotation(overrideOnlyAnnotationName)
      || isAnnotationPresent(overrideOnlyAnnotationName, context)

  private fun Method.isAnnotationPresent(annotationFqn: String, verificationContext: VerificationContext): Boolean {
    if (findEffectiveMemberAnnotation(annotationFqn, verificationContext.classResolver) != null) {
      return true
    }

    val overriddenMethod = searchParentOverrides(verificationContext.classResolver).firstOrNull { (overriddenMethod, c) ->
       overriddenMethod.findEffectiveMemberAnnotation(annotationFqn, verificationContext.classResolver) != null
    }
    return if (overriddenMethod == null) {
      LOG.atTrace().log("No overridden method for $name is annotated by [$annotationFqn]")
      false
    } else {
      LOG.atDebug().log("Method '${overriddenMethod.method.name}' in [${overriddenMethod.klass.name}] is annotated by [$annotationFqn]")
      true
    }
  }

  private companion object {
    const val overrideOnlyAnnotationName = "org/jetbrains/annotations/ApiStatus\$OverrideOnly"
  }

  private fun DelegateCallOnOverrideOnlyUsageFilter.withBridgeMethodSupport() =
    BridgeMethodOverrideUsageFilter(this)
}



