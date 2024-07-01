/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.toReference
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.FilteringApiUsageProcessor
import com.jetbrains.pluginverifier.usages.SamePluginUsageFilter
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.ProblemRegistrar
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.MethodResolver
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import com.jetbrains.pluginverifier.warnings.WarningRegistrar
import org.objectweb.asm.tree.AbstractInsnNode

class InternalApiUsageProcessor(private val pluginVerificationContext: PluginVerificationContext) : FilteringApiUsageProcessor(SamePluginUsageFilter()) {

  private fun isInternal(
    resolvedMember: ClassFileMember,
    context: VerificationContext,
    usageLocation: Location
  ): Boolean = resolvedMember.isInternalApi(context.classResolver)

  override fun doProcessClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType,
    context: VerificationContext
  ) {
    val usageLocation = referrer.location
    if (isInternal(resolvedClass, context, usageLocation)) {
      registerInternalApiUsage(InternalClassUsage(classReference, resolvedClass.location, usageLocation))
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
    if (isInternal(invokedMethod, context, usageLocation)) {
      // Check if the method is an override, and if so check top declaration
      val canBeOverridden = !invokedMethod.isStatic && !invokedMethod.isPrivate
        && invokedMethod.name != "<init>" && invokedMethod.name != "<clinit>"

      // Taken from MethodOverridingVerifier
      val overriddenMethod = if(canBeOverridden) {
        MethodResolver().resolveMethod(
          ClassFileWithNoMethodsWrapper(invokedMethod.containingClassFile),
          invokedMethod.location.toReference(),
          if (invokedMethod.containingClassFile.isInterface) Instruction.INVOKE_INTERFACE else Instruction.INVOKE_VIRTUAL,
          invokedMethod,
          VerificationContextWithSilentProblemRegistrar(context)
        )
      } else {
        null
      }

      if (overriddenMethod == null || isInternal(overriddenMethod, context, usageLocation)) {
        registerInternalApiUsage(InternalMethodUsage(invokedMethodReference, invokedMethod.location, usageLocation))
      }
    }
  }

  private class ClassFileWithNoMethodsWrapper(
    private val classFile: ClassFile
  ) : ClassFile by classFile {
    override val methods: Sequence<Method> get() = emptySequence()
  }

  private class VerificationContextWithSilentProblemRegistrar(
    private val delegate: VerificationContext
  ) : VerificationContext by delegate {
    override val problemRegistrar: ProblemRegistrar = object : ProblemRegistrar {
      override fun registerProblem(problem: CompatibilityProblem) = Unit
    }

    override val warningRegistrar: WarningRegistrar = object : WarningRegistrar {
      override fun registerCompatibilityWarning(warning: CompatibilityWarning) = Unit
    }
  }

  override fun doProcessFieldAccess(
    fieldReference: FieldReference,
    resolvedField: Field,
    callerMethod: Method,
    context: VerificationContext
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