package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.utils.BytecodeUtil
import com.jetbrains.pluginverifier.utils.checkClassExistsOrExternal
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.LocalVariableNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
class MethodLocalVarsVerifier : MethodVerifier {
  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    if (method.localVariables != null) {
      val localVariables = method.localVariables as List<LocalVariableNode>
      for (variable in localVariables) {
        val descr = BytecodeUtil.extractClassNameFromDescr(variable.desc) ?: continue
        ctx.checkClassExistsOrExternal(descr, { ctx.fromMethod(clazz, method) })
      }
    }


  }
}
