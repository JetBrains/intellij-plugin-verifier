package com.jetbrains.pluginverifier.verifiers.method

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.utils.VerificationContext
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.Type
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
class MethodReturnTypeVerifier : MethodVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, resolver: Resolver, ctx: VerificationContext) {
    val methodType = Type.getType(method.desc)
    val returnType = methodType.returnType

    val descriptor = returnType.descriptor
    if ("V" == descriptor) return  //void return type

    val returnTypeDesc = VerifierUtil.extractClassNameFromDescr(descriptor) ?: return

    VerifierUtil.checkClassExistsOrExternal(resolver, returnTypeDesc, ctx, { ctx.fromMethod(clazz, method) })
  }
}
