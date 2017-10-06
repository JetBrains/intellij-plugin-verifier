package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.misc.checkIfInterrupted
import com.jetbrains.pluginverifier.progress.ProgressIndicator
import com.jetbrains.pluginverifier.verifiers.clazz.AbstractMethodVerifier
import com.jetbrains.pluginverifier.verifiers.clazz.InheritFromFinalClassVerifier
import com.jetbrains.pluginverifier.verifiers.clazz.InterfacesVerifier
import com.jetbrains.pluginverifier.verifiers.clazz.SuperClassVerifier
import com.jetbrains.pluginverifier.verifiers.field.FieldTypeVerifier
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier
import com.jetbrains.pluginverifier.verifiers.instruction.*
import com.jetbrains.pluginverifier.verifiers.method.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class BytecodeVerifier {

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
      MethodTryCatchVerifier()
  )

  private val instructionVerifiers = arrayOf(
      InvokeInstructionVerifier(),
      TypeInstructionVerifier(),
      LdcInstructionVerifier(),
      MultiANewArrayInstructionVerifier(),
      FieldAccessInstructionVerifier()
  )

  fun verify(classesToCheck: Set<String>, verificationContext: VerificationContext, progressIndicator: ProgressIndicator) {
    if (classesToCheck.isNotEmpty()) {
      for ((totalVerifiedClasses, className) in classesToCheck.withIndex()) {
        checkIfInterrupted()
        verifyClass(className, verificationContext)
        progressIndicator.setProgress((totalVerifiedClasses + 1).toDouble() / classesToCheck.size)
      }
    }
  }

  private fun verifyClass(className: String, verificationContext: VerificationContext) {
    val node = try {
      verificationContext.classLoader.findClass(className)
    } catch (e: Exception) {
      null
    }
    if (node != null) {
      verifyClassNode(node, verificationContext)
    }
  }

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