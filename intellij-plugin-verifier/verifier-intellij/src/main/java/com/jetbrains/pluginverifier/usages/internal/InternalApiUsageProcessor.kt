/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.*
import org.objectweb.asm.tree.AbstractInsnNode

class InternalApiUsageProcessor(private val pluginVerificationContext: PluginVerificationContext) : ApiUsageProcessor {

  private fun isInternal(
    resolvedMember: ClassFileMember,
    context: VerificationContext,
    usageLocation: Location
  ) = resolvedMember.isInternalApi(context.classResolver)
    && resolvedMember.containingClassFile.classFileOrigin != usageLocation.containingClass.classFileOrigin

  override fun processClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    context: VerificationContext,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType
  ) {
    val usageLocation = referrer.location
    if (isInternal(resolvedClass, context, usageLocation)) {
      registerInternalApiUsage(InternalClassUsage(classReference, resolvedClass.location, usageLocation))
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
    if (isInternal(resolvedMethod, context, usageLocation)) {
      registerInternalApiUsage(InternalMethodUsage(methodReference, resolvedMethod.location, usageLocation))
    }
  }

  override fun processFieldAccess(
    fieldReference: FieldReference,
    resolvedField: Field,
    context: VerificationContext,
    callerMethod: Method
  ) {
    val usageLocation = callerMethod.location
    if (isInternal(resolvedField, context, usageLocation)) {
      registerInternalApiUsage(InternalFieldUsage(fieldReference, resolvedField.location, usageLocation))
    }
  }

  private fun registerInternalApiUsage(usage: InternalApiUsage) {
    // MP-3421 Plugin Verifier must report compatibility errors for usages of internal FUS APIs
    if (usage.apiElement.containingClass.packageName.startsWith("com/intellij/internal/statistic")
      && pluginVerificationContext.idePlugin.vendor?.contains("JetBrains", true) != true
      && pluginVerificationContext.verificationDescriptor is PluginVerificationDescriptor.IDE
      && pluginVerificationContext.verificationDescriptor.ideVersion.baselineVersion >= 211
    ) {
      pluginVerificationContext.registerProblem(InternalFusApiUsageCompatibilityProblem(usage))
    } else {
      pluginVerificationContext.registerInternalApiUsage(usage)
    }
  }
}