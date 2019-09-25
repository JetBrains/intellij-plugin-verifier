package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

interface ApiUsageProcessor {
  fun processClassReference(
      classReference: ClassReference,
      resolvedClass: ClassFile,
      context: VerificationContext,
      referrer: ClassFileMember
  ) = Unit

  fun processMethodInvocation(
      methodReference: MethodReference,
      resolvedMethod: Method,
      instructionNode: AbstractInsnNode,
      callerMethod: Method,
      context: VerificationContext
  ) = Unit

  fun processFieldAccess(
      fieldReference: FieldReference,
      resolvedField: Field,
      context: VerificationContext,
      callerMethod: Method
  ) = Unit

}