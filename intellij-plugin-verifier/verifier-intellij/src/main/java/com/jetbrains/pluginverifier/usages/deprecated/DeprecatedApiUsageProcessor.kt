/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.usages.SamePluginUsageFilter
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

class DeprecatedApiUsageProcessor(private val deprecatedApiRegistrar: DeprecatedApiRegistrar) : ApiUsageProcessor {
  private val allowedUsageFilter = SamePluginUsageFilter()

  /**
   * Process a reference to a [Class] from the API.
   *
   * Suppose that a `ApiConsumer` class has a method `consume()`:
   * ```
   *     public void consume() {
              new DeprecatedModuleApi();
   *     }
   * ```
   * @param classReference refers to the callee or the invocation target (`DeprecatedModuleApi`)
   * @param resolvedClass is a fully resolved model of the callee or the invocation target (`DeprecatedModuleApi`)
   * @param context indicates the verification context with additional data
   * @param referrer is a model corresponding to the caller. In this case, it is a `consume()` [Method] from the example
   * @param classUsageType indicates a usage type. In the example, it is a _default_ usage type.
   *
   */
  override fun processClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    context: VerificationContext,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType
  ) {
    val deprecationInfo = resolvedClass.deprecationInfo ?: return
    if (isIgnored(classReference, resolvedClass, referrer, classUsageType, context)) {
      return
    }
    deprecatedApiRegistrar.registerDeprecatedUsage(
      DeprecatedClassUsage(classReference, resolvedClass.location, referrer.location, deprecationInfo)
    )
  }

  override fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    instructionNode: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) {
    val deprecationInfo = resolvedMethod.deprecationInfo ?: return
    if (isIgnored(methodReference, resolvedMethod, instructionNode, callerMethod, context)) {
      return
    }
    deprecatedApiRegistrar.registerDeprecatedUsage(
      DeprecatedMethodUsage(methodReference, resolvedMethod.location, callerMethod.location, deprecationInfo)
    )
  }

  override fun processFieldAccess(
    fieldReference: FieldReference,
    resolvedField: Field,
    context: VerificationContext,
    callerMethod: Method
  ) {
    val deprecationInfo = resolvedField.deprecationInfo ?: return
    if (isIgnored(fieldReference, resolvedField, callerMethod, context)) {
      return
    }
    deprecatedApiRegistrar.registerDeprecatedUsage(
      DeprecatedFieldUsage(fieldReference, resolvedField.location, callerMethod.location, deprecationInfo)
    )
  }

  private fun isIgnored(
    fieldReference: FieldReference,
    resolvedField: Field,
    callerMethod: Method,
    context: VerificationContext
  ) = allowedUsageFilter.allowInvocation(fieldReference, resolvedField, callerMethod, context)

  private fun isIgnored(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType,
    context: VerificationContext
  ) = allowedUsageFilter.allowUsage(classReference, resolvedClass, referrer, classUsageType, context)


  private fun isIgnored(
    @Suppress("UNUSED_PARAMETER") invokedMethodReference: MethodReference,
    invokedMethod: Method,
    invocationInstruction: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext): Boolean {
    return allowedUsageFilter.allowMethodInvocation(invokedMethod, invocationInstruction, callerMethod, context)
  }

}