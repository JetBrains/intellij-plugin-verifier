package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.createMethodLocation
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LocalVariableNode
import org.objectweb.asm.tree.MethodNode

class MethodLocalVarsVerifier : MethodVerifier {
  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    if (method.localVariables != null) {
      val localVariables = method.localVariables as List<LocalVariableNode>
      for (variable in localVariables) {
        val descr = variable.desc.extractClassNameFromDescr() ?: continue
        ctx.resolveClassOrProblem(descr, clazz) { createMethodLocation(clazz, method) }
      }
    }


  }
}
