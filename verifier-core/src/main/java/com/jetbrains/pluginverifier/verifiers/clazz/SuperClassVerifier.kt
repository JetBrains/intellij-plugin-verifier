package com.jetbrains.pluginverifier.verifiers.clazz

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.IncompatibleClassToInterfaceChangeProblem
import com.jetbrains.pluginverifier.reference.SymbolicReference
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode

/**
 * Check that superclass exists.
 *
 * @author Dennis.Ushakov
 */
class SuperClassVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, resolver: Resolver, ctx: VContext) {
    val superClassName = if (clazz.superName == null) "java/lang/Object" else clazz.superName
    val aClass = VerifierUtil.resolveClassOrProblem(resolver, superClassName, clazz, ctx, { ProblemLocation.fromClass(clazz.name) }) ?: return
    if (VerifierUtil.isInterface(aClass)) {
      ctx.registerProblem(IncompatibleClassToInterfaceChangeProblem(SymbolicReference.classFrom(superClassName)), ProblemLocation.fromClass(clazz.name))
    }
  }
}
