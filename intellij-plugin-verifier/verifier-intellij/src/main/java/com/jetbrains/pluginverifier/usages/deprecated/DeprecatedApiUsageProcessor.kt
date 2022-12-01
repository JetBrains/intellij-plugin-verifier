/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.*
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode

class DeprecatedApiUsageProcessor(private val deprecatedApiRegistrar: DeprecatedApiRegistrar) : ApiUsageProcessor {
  override fun processClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    context: VerificationContext,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType
  ) {
    val deprecationInfo = resolvedClass.deprecationInfo ?: return
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
    deprecatedApiRegistrar.registerDeprecatedUsage(
      DeprecatedFieldUsage(fieldReference, resolvedField.location, callerMethod.location, deprecationInfo)
    )
  }
}