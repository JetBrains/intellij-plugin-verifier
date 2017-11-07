package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.checkClassExistsOrExternal
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import com.jetbrains.pluginverifier.verifiers.fromMethod
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LocalVariableNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
class MethodLocalVarsVerifier : MethodVerifier {
  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    if (method.localVariables != null) {
      val localVariables = method.localVariables as List<LocalVariableNode>
      for (variable in localVariables) {
        val descr = variable.desc.extractClassNameFromDescr() ?: continue
        ctx.checkClassExistsOrExternal(descr, { ctx.fromMethod(clazz, method) })
      }
    }


  }
}
