package com.jetbrains.pluginverifier.usages.experimental

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

class ExperimentalApiUsageProcessor(private val experimentalApiRegistrar: ExperimentalApiRegistrar) : ApiUsageProcessor {

  private fun isExperimental(
    resolvedMember: ClassFileMember,
    context: VerificationContext,
    usageLocation: Location
  ) = resolvedMember.isExperimentalApi(context.classResolver)
    && resolvedMember.containingClassFile.classFileOrigin != usageLocation.containingClass.classFileOrigin

  override fun processClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    usageLocation: Location,
    context: VerificationContext
  ) {
    if (isExperimental(resolvedClass, context, usageLocation)) {
      experimentalApiRegistrar.registerExperimentalApiUsage(
        ExperimentalClassUsage(classReference, resolvedClass.location, usageLocation)
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
    if (isExperimental(resolvedMethod, context, usageLocation)) {
      experimentalApiRegistrar.registerExperimentalApiUsage(
        ExperimentalMethodUsage(methodReference, resolvedMethod.location, usageLocation)
      )
    }
  }

  override fun processFieldAccess(
    fieldReference: FieldReference,
    resolvedField: Field,
    usageLocation: Location,
    context: VerificationContext
  ) {
    if (isExperimental(resolvedField, context, usageLocation)) {
      experimentalApiRegistrar.registerExperimentalApiUsage(
        ExperimentalFieldUsage(fieldReference, resolvedField.location, usageLocation)
      )
    }
  }
}