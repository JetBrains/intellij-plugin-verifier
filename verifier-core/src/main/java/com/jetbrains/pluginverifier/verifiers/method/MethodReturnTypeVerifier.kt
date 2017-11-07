package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.checkClassExistsOrExternal
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import com.jetbrains.pluginverifier.verifiers.fromMethod
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

    val returnTypeDesc = descriptor.extractClassNameFromDescr() ?: return

    ctx.checkClassExistsOrExternal(returnTypeDesc, { ctx.fromMethod(clazz, method) })
  }
}
