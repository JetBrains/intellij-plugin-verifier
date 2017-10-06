package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.BytecodeUtil
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.checkClassExistsOrExternal
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

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
