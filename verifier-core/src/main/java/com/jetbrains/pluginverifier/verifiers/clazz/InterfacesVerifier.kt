package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.problems.SuperInterfaceBecameClassProblem
import com.jetbrains.pluginverifier.utils.BytecodeUtil
import com.jetbrains.pluginverifier.utils.resolveClassOrProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext
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
        .filterNot { BytecodeUtil.isInterface(it) }
        .forEach { ctx.registerProblem(SuperInterfaceBecameClassProblem(ctx.fromClass(clazz), ctx.fromClass(it))) }
  }
}
