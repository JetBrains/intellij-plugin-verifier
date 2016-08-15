package com.jetbrains.pluginverifier.verifiers.method

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
class MethodArgumentTypesVerifier : MethodVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, resolver: Resolver, ctx: VContext) {
    val methodType = Type.getType(method.desc)
    val argumentTypes = methodType.argumentTypes
    for (type in argumentTypes) {
      val argDescr = VerifierUtil.extractClassNameFromDescr(type.descriptor) ?: continue
      VerifierUtil.checkClassExistsOrExternal(resolver, argDescr, ctx, { ProblemLocation.fromMethod(clazz.name, method) })
    }


  }
}
