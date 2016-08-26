package com.jetbrains.pluginverifier.verifiers.clazz

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.InheritFromFinalClassProblem
import com.jetbrains.pluginverifier.reference.ClassReference
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.tree.ClassNode

class InheritFromFinalClassVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, resolver: Resolver, ctx: VContext) {
    val superClassName = if (clazz.superName == null) "java/lang/Object" else clazz.superName
    val supClass = VerifierUtil.resolveClassOrProblem(resolver, superClassName, clazz, ctx, { ProblemLocation.fromClass(clazz.name) }) ?: return
    if (VerifierUtil.isFinal(supClass)) {
      ctx.registerProblem(InheritFromFinalClassProblem(ClassReference(supClass.name)), ProblemLocation.fromClass(clazz.name))
    }
  }
}
