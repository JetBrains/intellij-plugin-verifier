/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.method.KotlinMethods.isKotlinDefaultMethod
import com.jetbrains.pluginverifier.verifiers.resolution.*
import org.objectweb.asm.tree.AbstractInsnNode

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

/**
 * Whether the API usages inside this member are passed to the [ApiUsageProcessor]s.
 *
 * They are not for the bridges the Kotlin compiler synthesizes for inherited interface default
 * methods, see [isKotlinDefaultMethod]: the plugin author did not write them, so they must not be
 * reported as usages of deprecated, experimental, internal or otherwise restricted API. Problems
 * that the instruction verifiers detect themselves, such as a missing method, are still reported.
 */
internal val ClassFileMember.hasApiUsagesToProcess: Boolean
  get() = this !is Method || !isKotlinDefaultMethod()
