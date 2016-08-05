package com.jetbrains.pluginverifier.verifiers.clazz

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.problems.IncompatibleClassToInterfaceChange
import com.jetbrains.pluginverifier.reference.ClassReference
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.tree.ClassNode

/**
 * Check that superclass exists.
 *
 * @author Dennis.Ushakov
 */
class SuperClassVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, resolver: Resolver, ctx: VContext) {
    val superClassName = if (clazz.superName == null) "java/lang/Object" else clazz.superName
    val aClass = VerifierUtil.findClass(resolver, superClassName, ctx)
    if (aClass == null) {
      if (!ctx.verifierOptions.isExternalClass(superClassName)) {
        ctx.registerProblem(ClassNotFoundProblem(superClassName), ProblemLocation.fromClass(clazz.name))
      }
      return
    }
    if (VerifierUtil.isInterface(aClass)) {
      ctx.registerProblem(IncompatibleClassToInterfaceChange(ClassReference(superClassName)), ProblemLocation.fromClass(clazz.name))
    }
  }
}
