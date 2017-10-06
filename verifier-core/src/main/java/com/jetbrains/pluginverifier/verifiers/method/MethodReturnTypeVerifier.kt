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
class MethodReturnTypeVerifier : MethodVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    val methodType = Type.getType(method.desc)
    val returnType = methodType.returnType

    val descriptor = returnType.descriptor
    if ("V" == descriptor) return  //void return type

    val returnTypeDesc = BytecodeUtil.extractClassNameFromDescr(descriptor) ?: return

    ctx.checkClassExistsOrExternal(returnTypeDesc, { ctx.fromMethod(clazz, method) })
  }
}
