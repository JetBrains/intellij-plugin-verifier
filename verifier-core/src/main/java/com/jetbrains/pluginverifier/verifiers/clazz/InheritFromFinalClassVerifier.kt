package com.jetbrains.pluginverifier.verifiers.clazz

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.problems.InheritFromFinalClassProblem
import com.jetbrains.pluginverifier.reference.SymbolicReference
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode

class InheritFromFinalClassVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, resolver: Resolver, ctx: VContext) {
    val superClassName = if (clazz.superName == null) "java/lang/Object" else clazz.superName
    val supClass = VerifierUtil.resolveClassOrProblem(resolver, superClassName, clazz, ctx, { VerifierUtil.fromClass(clazz) }) ?: return
    if (VerifierUtil.isFinal(supClass)) {
      ctx.registerProblem(InheritFromFinalClassProblem(SymbolicReference.classFrom(supClass.name)), VerifierUtil.fromClass(clazz))
    }
  }
}
