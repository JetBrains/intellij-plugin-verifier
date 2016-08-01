package com.jetbrains.pluginverifier.verifiers.clazz

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.problems.InheritFromFinalClassProblem
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.tree.ClassNode

class InheritFromFinalClassVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, resolver: Resolver, ctx: VContext) {
    val superClassName = if (clazz.superName == null) "java/lang/Object" else clazz.superName
    val supClass = VerifierUtil.findClass(resolver, superClassName, ctx)
    if (supClass == null) {
      if (!ctx.verifierOptions.isExternalClass(superClassName)) {
        ctx.registerProblem(ClassNotFoundProblem(superClassName), ProblemLocation.fromClass(clazz.name))
      }
      return
    }
    if (VerifierUtil.isFinal(supClass)) {
      ctx.registerProblem(InheritFromFinalClassProblem(supClass.name), ProblemLocation.fromClass(clazz.name))
    }
  }
}
