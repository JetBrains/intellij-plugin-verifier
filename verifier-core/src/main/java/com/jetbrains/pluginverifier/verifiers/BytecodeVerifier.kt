package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.misc.checkIfInterrupted
import com.jetbrains.pluginverifier.verifiers.clazz.AbstractMethodVerifier
import com.jetbrains.pluginverifier.verifiers.clazz.InheritFromFinalClassVerifier
import com.jetbrains.pluginverifier.verifiers.clazz.InterfacesVerifier
import com.jetbrains.pluginverifier.verifiers.clazz.SuperClassVerifier
import com.jetbrains.pluginverifier.verifiers.field.FieldTypeVerifier
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter
import com.jetbrains.pluginverifier.verifiers.instruction.*
import com.jetbrains.pluginverifier.verifiers.method.*
import com.jetbrains.pluginverifier.verifiers.resolution.ClsResolution
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * Bytecode verification entry point, which takes as input
 * a [VerificationContext] and classes for verification, and
 * runs available bytecode verifiers against such configuration.
 */
class BytecodeVerifier {

  private val verificationFilters = listOf(DynamicallyLoadedFilter())

  private val fieldVerifiers = arrayOf<FieldVerifier>(FieldTypeVerifier())

  private val classVerifiers = arrayOf(
      SuperClassVerifier(),
      InterfacesVerifier(),
      AbstractMethodVerifier(),
      InheritFromFinalClassVerifier()
  )

  private val methodVerifiers = arrayOf(
      OverrideNonFinalVerifier(),
      MethodReturnTypeVerifier(),
      MethodArgumentTypesVerifier(),
      MethodLocalVarsVerifier(),
      MethodThrowsVerifier(),
      MethodTryCatchVerifier(),
      UnstableMethodOverriddenVerifier()
  )

  private val instructionVerifiers = arrayOf(
      InvokeInstructionVerifier(),
      TypeInstructionVerifier(),
      LdcInstructionVerifier(),
      MultiANewArrayInstructionVerifier(),
      FieldAccessInstructionVerifier()
  )

  /**
   * Runs bytecode verification for [classesToCheck]
   * using the [verificationContext].
   * Updates [progressIndicator] with percentage
   * of processed classes.
   */
  @Throws(InterruptedException::class)
  fun verify(
      classesToCheck: Set<String>,
      verificationContext: VerificationContext,
      progressIndicator: (Double) -> Unit
  ) {
    if (classesToCheck.isNotEmpty()) {
      for ((totalVerifiedClasses, className) in classesToCheck.withIndex()) {
        checkIfInterrupted()
        verifyClass(className, verificationContext)
        progressIndicator((totalVerifiedClasses + 1).toDouble() / classesToCheck.size)
      }
    }
  }

  private fun verifyClass(className: String, verificationContext: VerificationContext) {
    val clsResolution = verificationContext.clsResolver.resolveClass(className)
    if (clsResolution is ClsResolution.Found && shouldVerify(clsResolution.node)) {
      verifyClassNode(clsResolution.node, verificationContext)
    }
  }

  private fun shouldVerify(classNode: ClassNode) = verificationFilters.all { it.shouldVerify(classNode) }

  @Suppress("UNCHECKED_CAST")
  private fun verifyClassNode(node: ClassNode, ctx: VerificationContext) {
    for (verifier in classVerifiers) {
      verifier.verify(node, ctx)
    }

    val methods = node.methods as List<MethodNode>
    for (method in methods) {
      for (verifier in methodVerifiers) {
        verifier.verify(node, method, ctx)
      }

      method.instructions.iterator().forEach { instruction ->
        for (verifier in instructionVerifiers) {
          verifier.verify(node, method, instruction as AbstractInsnNode, ctx)
        }
      }
    }

    val fields = node.fields as List<FieldNode>
    for (field in fields) {
      for (verifier in fieldVerifiers) {
        verifier.verify(node, field, ctx)
      }
    }
  }

}