package com.jetbrains.pluginverifier.verifiers.method

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TryCatchBlockNode

/**
 * @author Sergey Patrikeev
 */
class MethodTryCatchVerifier : MethodVerifier {
  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, resolver: Resolver, ctx: VContext) {
    val blocks = method.tryCatchBlocks as List<TryCatchBlockNode>
    for (block in blocks) {
      val catchException = block.type ?: continue
      val descr = VerifierUtil.extractClassNameFromDescr(catchException) ?: continue
      VerifierUtil.checkClassExistsOrExternal(resolver, descr, ctx, { ProblemLocation.fromMethod(clazz.name, method) })
    }
  }
}
