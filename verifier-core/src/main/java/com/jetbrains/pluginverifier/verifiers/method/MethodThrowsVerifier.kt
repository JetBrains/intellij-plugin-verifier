package com.jetbrains.pluginverifier.verifiers.method

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
class MethodThrowsVerifier : MethodVerifier {
  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, resolver: Resolver, ctx: VContext) {
    val exceptions = method.exceptions as List<String>
    for (exception in exceptions) {
      val descr = VerifierUtil.extractClassNameFromDescr(exception) ?: continue
      if (!VerifierUtil.classExistsOrExternal(ctx, resolver, descr)) {
        ctx.registerProblem(ClassNotFoundProblem(descr), ProblemLocation.fromMethod(clazz.name, method))
      }
    }
  }
}
