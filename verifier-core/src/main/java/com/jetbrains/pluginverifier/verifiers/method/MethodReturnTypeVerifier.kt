package com.jetbrains.pluginverifier.verifiers.method

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
class MethodReturnTypeVerifier : MethodVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, resolver: Resolver, ctx: VContext) {
    val methodType = Type.getType(method.desc)
    val returnType = methodType.returnType

    val descriptor = returnType.descriptor
    if ("V" == descriptor) return  //void return type

    val returnTypeDesc = VerifierUtil.extractClassNameFromDescr(descriptor) ?: return

    if (!VerifierUtil.classExistsOrExternal(ctx, resolver, returnTypeDesc)) {
      ctx.registerProblem(ClassNotFoundProblem(returnTypeDesc), ProblemLocation.fromMethod(clazz.name, method))
    }

  }
}
