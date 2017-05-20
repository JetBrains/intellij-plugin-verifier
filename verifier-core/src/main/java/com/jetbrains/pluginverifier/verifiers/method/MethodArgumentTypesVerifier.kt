package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.utils.BytecodeUtil
import com.jetbrains.pluginverifier.utils.checkClassExistsOrExternal
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.jetbrains.intellij.plugins.internal.asm.Type
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
class MethodArgumentTypesVerifier : MethodVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    val methodType = Type.getType(method.desc)
    val argumentTypes = methodType.argumentTypes
    for (type in argumentTypes) {
      val argDescr = BytecodeUtil.extractClassNameFromDescr(type.descriptor) ?: continue
      ctx.checkClassExistsOrExternal(argDescr, { ctx.fromMethod(clazz, method) })
    }


  }
}
