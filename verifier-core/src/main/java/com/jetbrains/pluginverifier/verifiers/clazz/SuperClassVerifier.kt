package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.problems.SuperClassBecameInterfaceProblem
import com.jetbrains.pluginverifier.verifiers.BytecodeUtil
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.tree.ClassNode

/**
 * Check that superclass exists.
 *
 * @author Dennis.Ushakov
 */
class SuperClassVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, ctx: VerificationContext) {
    val superClassName = clazz.superName ?: "java/lang/Object"
    val superNode = ctx.resolveClassOrProblem(superClassName, clazz, { ctx.fromClass(clazz) }) ?: return
    if (BytecodeUtil.isInterface(superNode)) {
      ctx.registerProblem(SuperClassBecameInterfaceProblem(ctx.fromClass(clazz), ctx.fromClass(superNode)))
    }
  }
}
