package com.jetbrains.pluginverifier.verifiers.clazz

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.problems.IncompatibleInterfaceToClassChange
import com.jetbrains.pluginverifier.reference.ClassReference
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.tree.ClassNode

/**
 * Check that all explicitly defined interfaces exists.

 * @author Dennis.Ushakov
 */
class InterfacesVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, resolver: Resolver, ctx: VContext) {
    for (o in clazz.interfaces) {
      val iface = o as String
      val node = VerifierUtil.findClass(resolver, iface, ctx)
      if (node == null) {
        if (!ctx.verifierOptions.isExternalClass(iface)) {
          ctx.registerProblem(ClassNotFoundProblem(iface), ProblemLocation.fromClass(clazz.name))
        }
        continue
      }
      if (!VerifierUtil.isInterface(node)) {
        ctx.registerProblem(IncompatibleInterfaceToClassChange(ClassReference(iface)), ProblemLocation.fromClass(clazz.name))
      }
    }
  }
}
