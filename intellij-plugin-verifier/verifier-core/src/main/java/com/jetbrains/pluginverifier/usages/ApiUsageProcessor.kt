package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

interface ApiUsageProcessor {
  fun processClassReference(
      classReference: ClassReference,
      resolvedClass: ClassFile,
      usageLocation: Location,
      context: VerificationContext
  ) = Unit

  fun processMethodInvocation(
      methodReference: MethodReference,
      resolvedMethod: Method,
      usageLocation: Location,
      context: VerificationContext,
      instructionNode: AbstractInsnNode
  ) = Unit

  fun processFieldAccess(
      fieldReference: FieldReference,
      resolvedField: Field,
      usageLocation: Location,
      context: VerificationContext
  ) = Unit

}