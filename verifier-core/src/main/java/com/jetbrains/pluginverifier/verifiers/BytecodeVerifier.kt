package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.api.VerifierParams
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BytecodeVerifier(verifierParams: VerifierParams,
                       plugin: IdePlugin,
                       classLoader: Resolver,
                       private val ideVersion: IdeVersion) {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(BytecodeVerifier::class.java)
  }

  private val verificationContext: VerificationContext = VerificationContext(plugin, verifierParams, classLoader)

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

  private var totalVerifiedClasses: Int = 0

  fun verify(classesToCheck: Set<String>): VerificationContext {
    totalVerifiedClasses = 0
    for (className in classesToCheck) {
      checkCancelled()
      verifyClass(className)
      totalVerifiedClasses++
      if (totalVerifiedClasses % 1000 == 0) {
        LOG.debug("'${verificationContext.plugin}' and #$ideVersion: finished $totalVerifiedClasses classes")
      }
    }
    return verificationContext
  }

  private fun checkCancelled() {
    if (Thread.currentThread().isInterrupted) {
      throw InterruptedException("The verification was cancelled")
    }
  }

  private fun verifyClass(className: String) {
    val node = try {
      verificationContext.resolver.findClass(className)
    } catch (e: Exception) {
      null
    }
    if (node != null) {
      verifyClass(node, verificationContext)
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