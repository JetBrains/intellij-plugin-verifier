/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.usages.util.isFromVerifiedPlugin
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

class ExperimentalApiUsageProcessor(private val experimentalApiRegistrar: ExperimentalApiRegistrar) : ApiUsageProcessor {

  private fun isExperimental(
    resolvedMember: ClassFileMember,
    context: VerificationContext,
    usageLocation: Location
  ) = resolvedMember.isExperimentalApi(context.classResolver, usageLocation)
    && resolvedMember.containingClassFile.classFileOrigin != usageLocation.containingClass.classFileOrigin

  override fun processClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    context: VerificationContext,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType
  ) {
    val usageLocation = referrer.location
    if (isExperimental(resolvedClass, context, usageLocation) && context.isFromVerifiedPlugin(referrer)) {
      experimentalApiRegistrar.registerExperimentalApiUsage(
        ExperimentalClassUsage(classReference, resolvedClass.location, usageLocation)
      )
    }
  }

  override fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    instructionNode: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) {
    val usageLocation = callerMethod.location
    if (isExperimental(resolvedMethod, context, usageLocation)) {
      experimentalApiRegistrar.registerExperimentalApiUsage(
        ExperimentalMethodUsage(methodReference, resolvedMethod.location, usageLocation)
      )
    }
  }

  override fun processFieldAccess(
    fieldReference: FieldReference,
    resolvedField: Field,
    context: VerificationContext,
    callerMethod: Method
  ) {
    val usageLocation = callerMethod.location
    if (isExperimental(resolvedField, context, usageLocation)) {
      experimentalApiRegistrar.registerExperimentalApiUsage(
        ExperimentalFieldUsage(fieldReference, resolvedField.location, callerMethod.location)
      )
    }
  }
}