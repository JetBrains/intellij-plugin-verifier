package com.jetbrains.pluginverifier.verifiers.filter

import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

class CompositeApiUsageFilter(private val apiUsageFilters: List<ApiUsageFilter>) : ApiUsageFilter {
  constructor(vararg filters: ApiUsageFilter) : this(filters.toList())

  override fun allow(invokedMethod: Method, invocationInstruction: AbstractInsnNode, callerMethod: Method, context: VerificationContext): Boolean {
    return apiUsageFilters.any {
      it.allow(invokedMethod, invocationInstruction, callerMethod, context)
    }
  }

  override fun allow(
    classReference: ClassReference,
    invocationTarget: ClassFile,
    caller: ClassFileMember,
    usageType: ClassUsageType,
    context: VerificationContext
  ): Boolean {
    return apiUsageFilters.any {
      it.allow(classReference, invocationTarget, caller, usageType, context)
    }
  }

  override fun allow(
    fieldReference: FieldReference,
    resolvedField: Field,
    callerMethod: Method,
    context: VerificationContext
  ): Boolean {
    return apiUsageFilters.any {
      it.allow(fieldReference, resolvedField, callerMethod, context)
    }
  }
}