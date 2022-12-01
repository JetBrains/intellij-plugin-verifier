/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.*
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode

interface ApiUsageProcessor {
  fun processClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    context: VerificationContext,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType
  ) = Unit

  fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    instructionNode: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) = Unit

  fun processFieldAccess(
    fieldReference: FieldReference,
    resolvedField: Field,
    context: VerificationContext,
    callerMethod: Method
  ) = Unit

}