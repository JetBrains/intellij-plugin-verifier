package com.jetbrains.pluginverifier.verifiers.method

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.utils.VerificationContext
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode
import org.jetbrains.intellij.plugins.internal.asm.tree.TryCatchBlockNode

/**
 * @author Sergey Patrikeev
 */
class MethodTryCatchVerifier : MethodVerifier {
  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, resolver: Resolver, ctx: VerificationContext) {
    val blocks = method.tryCatchBlocks as List<TryCatchBlockNode>
    for (block in blocks) {
      val catchException = block.type ?: continue
      val descr = VerifierUtil.extractClassNameFromDescr(catchException) ?: continue
      VerifierUtil.checkClassExistsOrExternal(resolver, descr, ctx, { ctx.fromMethod(clazz, method) })
    }
  }
}
