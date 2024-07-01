package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

abstract class FilteringApiUsageProcessor(private val usageFilter: ApiUsageFilter) : ApiUsageProcessor {
  final override fun processClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    context: VerificationContext,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType
  ) {
    if (usageFilter.allow(classReference, resolvedClass, referrer, classUsageType, context)) return
    doProcessClassReference(classReference, resolvedClass, referrer, classUsageType, context)
  }

  final override fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    instructionNode: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) {
    if (usageFilter.allow(resolvedMethod, instructionNode, callerMethod, context)) return
    doProcessMethodInvocation(methodReference, resolvedMethod, instructionNode, callerMethod, context)
  }

  final override fun processFieldAccess(
    fieldReference: FieldReference,
    resolvedField: Field,
    context: VerificationContext,
    callerMethod: Method
  ) {
    if (usageFilter.allow(fieldReference, resolvedField, callerMethod, context)) return
    doProcessFieldAccess(fieldReference, resolvedField, callerMethod, context)
  }

  protected abstract fun doProcessClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType,
    context: VerificationContext,
  )

  protected abstract fun doProcessMethodInvocation(
    invokedMethodReference: MethodReference,
    invokedMethod: Method,
    invocationInstruction: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  )

  protected abstract fun doProcessFieldAccess(
    fieldReference: FieldReference,
    resolvedField: Field,
    callerMethod: Method,
    context: VerificationContext
  )
}