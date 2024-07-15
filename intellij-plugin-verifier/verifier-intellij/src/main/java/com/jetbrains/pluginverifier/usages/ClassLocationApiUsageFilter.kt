package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

open class ClassLocationApiUsageFilter : ApiUsageFilter {
  override fun allow(
    classReference: ClassReference,
    invocationTarget: ClassFile,
    caller: ClassFileMember,
    usageType: ClassUsageType,
    context: VerificationContext
  ): Boolean {
    val usageHost = caller.location.containingClass
    val apiHost = invocationTarget.location.containingClass
    return allow(usageHost, apiHost)
  }

  override fun allow(
    invokedMethod: Method,
    invocationInstruction: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ): Boolean {
    val usageHost = callerMethod.location.containingClass
    val apiHost = invokedMethod.location.containingClass
    return allow(usageHost, apiHost)
  }

  override fun allow(
    fieldReference: FieldReference,
    resolvedField: Field,
    callerMethod: Method,
    context: VerificationContext
  ): Boolean {
    val apiHost = callerMethod.location.containingClass
    val usageHost = resolvedField.location.containingClass
    return allow(usageHost, apiHost)
  }

  open fun allow(usageLocation: ClassLocation, apiLocation: ClassLocation): Boolean = true
}