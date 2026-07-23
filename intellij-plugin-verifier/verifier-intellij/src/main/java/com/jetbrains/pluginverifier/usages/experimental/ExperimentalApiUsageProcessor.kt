/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.FilteringApiUsageProcessor
import com.jetbrains.pluginverifier.usages.SamePluginUsageFilter
import com.jetbrains.pluginverifier.usages.util.KotlinInlinedCodeDetector
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

class ExperimentalApiUsageProcessor(private val experimentalApiRegistrar: ExperimentalApiRegistrar) : FilteringApiUsageProcessor(
  SamePluginUsageFilter()
) {

  private val inlinedCodeDetector = KotlinInlinedCodeDetector()

  private fun isExperimental(
    resolvedMember: ClassFileMember,
    context: VerificationContext,
    usageLocation: Location
  ) = resolvedMember.isExperimentalApi(context.classResolver, usageLocation)

  override fun doProcessClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType,
    context: VerificationContext,
    instructionNode: AbstractInsnNode?
  ) {
    val usageLocation = referrer.location
    if (isExperimental(resolvedClass, context, usageLocation)) {
      if (instructionNode != null && referrer is Method
        && inlinedCodeDetector.isInlinedFromOutsidePlugin(instructionNode, referrer, context)) {
        return
      }

      experimentalApiRegistrar.registerExperimentalApiUsage(
        ExperimentalClassUsage(classReference, resolvedClass.location, usageLocation)
      )
    }
  }

  override fun doProcessMethodInvocation(
    invokedMethodReference: MethodReference,
    invokedMethod: Method,
    invocationInstruction: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) {
    val usageLocation = callerMethod.location
    if (isExperimental(invokedMethod, context, usageLocation)) {
      if (inlinedCodeDetector.isInlinedFromOutsidePlugin(invocationInstruction, callerMethod, context)) {
        return
      }

      experimentalApiRegistrar.registerExperimentalApiUsage(
        ExperimentalMethodUsage(invokedMethodReference, invokedMethod.location, usageLocation)
      )
    }
  }

  override fun doProcessFieldAccess(
    fieldReference: FieldReference,
    resolvedField: Field,
    callerMethod: Method,
    context: VerificationContext
  ) {
    val usageLocation = callerMethod.location
    if (isExperimental(resolvedField, context, usageLocation)) {
      experimentalApiRegistrar.registerExperimentalApiUsage(
        ExperimentalFieldUsage(fieldReference, resolvedField.location, callerMethod.location)
      )
    }
  }

}