package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.problems.SuperInterfaceBecameClassProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.fromClass
import com.jetbrains.pluginverifier.verifiers.isInterface
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.tree.ClassNode

/**
 * Check that all explicitly defined interfaces exist.

 * @author Dennis.Ushakov
 */
class InterfacesVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, ctx: VerificationContext) {
    clazz.interfaces
        .filterIsInstance(String::class.java)
        .mapNotNull { ctx.resolveClassOrProblem(it, clazz, { ctx.fromClass(clazz) }) }
        .filterNot { it.isInterface() }
        .forEach { ctx.registerProblem(SuperInterfaceBecameClassProblem(ctx.fromClass(clazz), ctx.fromClass(it))) }
  }
}
