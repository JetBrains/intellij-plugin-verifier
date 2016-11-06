package com.jetbrains.pluginverifier.verifiers.clazz

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.IncompatibleInterfaceToClassChangeProblem
import com.jetbrains.pluginverifier.reference.SymbolicReference
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode

/**
 * Check that all explicitly defined interfaces exists.

 * @author Dennis.Ushakov
 */
class InterfacesVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, resolver: Resolver, ctx: VContext) {
    for (o in clazz.interfaces) {
      val iface = o as String
      val node = VerifierUtil.resolveClassOrProblem(resolver, iface, clazz, ctx, { ProblemLocation.fromClass(clazz.name) }) ?: continue
      if (!VerifierUtil.isInterface(node)) {
        ctx.registerProblem(IncompatibleInterfaceToClassChangeProblem(SymbolicReference.classFrom(iface)), ProblemLocation.fromClass(clazz.name))
      }
    }
  }
}
