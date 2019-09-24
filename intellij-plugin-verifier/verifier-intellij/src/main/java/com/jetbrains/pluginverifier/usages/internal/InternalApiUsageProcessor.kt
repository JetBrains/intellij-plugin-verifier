package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

class InternalApiUsageProcessor(private val internalApiRegistrar: InternalApiUsageRegistrar) : ApiUsageProcessor {

  private fun isInternal(
    resolvedMember: ClassFileMember,
    context: VerificationContext,
    usageLocation: Location
  ) = resolvedMember.isInternalApi(context.classResolver)
    && resolvedMember.containingClassFile.classFileOrigin != usageLocation.containingClass.classFileOrigin

  override fun processClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    usageLocation: Location,
    context: VerificationContext
  ) {
    if (isInternal(resolvedClass, context, usageLocation)) {
      internalApiRegistrar.registerInternalApiUsage(
        InternalClassUsage(classReference, resolvedClass.location, usageLocation)
      )
    }
  }

  override fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    usageLocation: Location,
    context: VerificationContext,
    instructionNode: AbstractInsnNode
  ) {
    if (isInternal(resolvedMethod, context, usageLocation)) {
      internalApiRegistrar.registerInternalApiUsage(
        InternalMethodUsage(methodReference, resolvedMethod.location, usageLocation)
      )
    }
  }

  override fun processFieldAccess(
    fieldReference: FieldReference,
    resolvedField: Field,
    usageLocation: Location,
    context: VerificationContext
  ) {
    if (isInternal(resolvedField, context, usageLocation)) {
      internalApiRegistrar.registerInternalApiUsage(
        InternalFieldUsage(fieldReference, resolvedField.location, usageLocation)
      )
    }
  }
}