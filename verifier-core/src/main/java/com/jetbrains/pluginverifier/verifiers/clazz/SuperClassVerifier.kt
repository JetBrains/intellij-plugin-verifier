package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.problems.SuperClassBecameInterfaceProblem
import com.jetbrains.pluginverifier.utils.VerificationContext
import com.jetbrains.pluginverifier.utils.VerifierUtil
import com.jetbrains.pluginverifier.utils.resolveClassOrProblem
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode

/**
 * Check that superclass exists.
 *
 * @author Dennis.Ushakov
 */
class SuperClassVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, ctx: VerificationContext) {
    val superClassName = clazz.superName ?: "java/lang/Object"
    val superNode = ctx.resolveClassOrProblem(superClassName, clazz, { ctx.fromClass(clazz) }) ?: return
    if (VerifierUtil.isInterface(superNode)) {
      ctx.registerProblem(SuperClassBecameInterfaceProblem(ctx.fromClass(clazz), ctx.fromClass(superNode)))
    }
  }
}
