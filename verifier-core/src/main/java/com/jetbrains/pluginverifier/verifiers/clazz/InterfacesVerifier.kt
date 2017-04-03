package com.jetbrains.pluginverifier.verifiers.clazz

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.problems.SuperInterfaceBecameClassProblem
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode

/**
 * Check that all explicitly defined interfaces exist.

 * @author Dennis.Ushakov
 */
class InterfacesVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, resolver: Resolver, ctx: VContext) {
    clazz.interfaces
        .filterIsInstance(String::class.java)
        .mapNotNull { VerifierUtil.resolveClassOrProblem(resolver, it, clazz, ctx, { ctx.fromClass(clazz) }) }
        .filterNot { VerifierUtil.isInterface(it) }
        .forEach { ctx.registerProblem(SuperInterfaceBecameClassProblem(ctx.fromClass(clazz), ctx.fromClass(it))) }
  }
}
