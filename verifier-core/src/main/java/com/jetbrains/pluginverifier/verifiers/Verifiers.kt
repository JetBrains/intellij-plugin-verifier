package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.utils.VerificationContext
import com.jetbrains.pluginverifier.verifiers.clazz.AbstractMethodVerifier
import com.jetbrains.pluginverifier.verifiers.clazz.InheritFromFinalClassVerifier
import com.jetbrains.pluginverifier.verifiers.clazz.InterfacesVerifier
import com.jetbrains.pluginverifier.verifiers.clazz.SuperClassVerifier
import com.jetbrains.pluginverifier.verifiers.field.FieldTypeVerifier
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier
import com.jetbrains.pluginverifier.verifiers.instruction.*
import com.jetbrains.pluginverifier.verifiers.method.*
import org.jetbrains.intellij.plugins.internal.asm.tree.AbstractInsnNode
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.FieldNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode

class BytecodeVerifier(val ctx: VerificationContext) {

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

  fun verify(classesToCheck: Set<String>) {
    classesToCheck.forEach {
      if (Thread.currentThread().isInterrupted) {
        throw InterruptedException("The verification was cancelled")
      }
      val node = try {
        ctx.resolver.findClass(it)
      } catch (e: Exception) {
        null
      }
      if (node != null) {
        verifyClass(node, ctx)
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun verifyClass(node: ClassNode, ctx: VerificationContext) {
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