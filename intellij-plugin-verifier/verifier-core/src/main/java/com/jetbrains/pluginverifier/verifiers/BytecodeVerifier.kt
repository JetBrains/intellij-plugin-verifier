package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.base.utils.checkIfInterrupted
import com.jetbrains.pluginverifier.verifiers.clazz.*
import com.jetbrains.pluginverifier.verifiers.field.FieldTypeVerifier
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier
import com.jetbrains.pluginverifier.verifiers.filter.ClassFilter
import com.jetbrains.pluginverifier.verifiers.instruction.*
import com.jetbrains.pluginverifier.verifiers.method.*
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull

class BytecodeVerifier(
  private val verificationFilters: List<ClassFilter> = emptyList(),
  additionalClassVerifiers: List<ClassVerifier> = emptyList(),
  additionalMethodVerifiers: List<MethodVerifier> = emptyList(),
  additionalFieldVerifiers: List<FieldVerifier> = emptyList(),
  additionalInstructionVerifiers: List<InstructionVerifier> = emptyList()
) {

  private val fieldVerifiers = listOf<FieldVerifier>(FieldTypeVerifier()) + additionalFieldVerifiers

  private val classVerifiers = listOf(
    SuperClassVerifier(),
    InterfacesVerifier(),
    AbstractMethodVerifier(),
    InheritFromFinalClassVerifier()
  ) + additionalClassVerifiers

  private val methodVerifiers = listOf(
    OverrideNonFinalVerifier(),
    MethodReturnTypeVerifier(),
    MethodArgumentTypesVerifier(),
    MethodLocalVarsVerifier(),
    MethodThrowsVerifier(),
    MethodTryCatchVerifier()
  ) + additionalMethodVerifiers

  private val instructionVerifiers = listOf(
    MemberAccessVerifier(),
    TypeInstructionVerifier(),
    LdcInstructionVerifier(),
    MultiANewArrayInstructionVerifier()
  ) + additionalInstructionVerifiers

  @Throws(InterruptedException::class)
  fun verify(
    classesToCheck: Set<String>,
    context: VerificationContext,
    progressIndicator: (Double) -> Unit
  ) {
    if (classesToCheck.isNotEmpty()) {
      for ((totalVerifiedClasses, className) in classesToCheck.withIndex()) {
        checkIfInterrupted()
        verifyClass(className, context)
        progressIndicator((totalVerifiedClasses + 1).toDouble() / classesToCheck.size)
      }
    }
  }

  private fun verifyClass(className: String, context: VerificationContext) {
    val classFile = context.classResolver.resolveClassOrNull(className)
    if (classFile != null && shouldVerify(classFile)) {
      verifyClassFile(classFile, context)
    }
  }

  private fun shouldVerify(classFile: ClassFile) = verificationFilters.all { it.shouldVerify(classFile) }

  private fun verifyClassFile(classFile: ClassFile, context: VerificationContext) {
    for (verifier in classVerifiers) {
      verifier.verify(classFile, context)
    }

    for (method in classFile.methods) {
      for (verifier in methodVerifiers) {
        verifier.verify(method, context)
      }

      method.instructions.forEach { instruction ->
        for (verifier in instructionVerifiers) {
          verifier.verify(method, instruction, context)
        }
      }
    }

    for (field in classFile.fields) {
      for (verifier in fieldVerifiers) {
        verifier.verify(field, context)
      }
    }
  }

}