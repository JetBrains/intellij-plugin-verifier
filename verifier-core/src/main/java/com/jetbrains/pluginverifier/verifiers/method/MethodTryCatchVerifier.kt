package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.createMethodLocation
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TryCatchBlockNode

class MethodTryCatchVerifier : MethodVerifier {
  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    val blocks = method.tryCatchBlocks as List<TryCatchBlockNode>
    for (block in blocks) {
      val catchException = block.type ?: continue
      val descr = catchException.extractClassNameFromDescr() ?: continue
      ctx.resolveClassOrProblem(descr, clazz) { createMethodLocation(clazz, method) }
    }
  }
}
