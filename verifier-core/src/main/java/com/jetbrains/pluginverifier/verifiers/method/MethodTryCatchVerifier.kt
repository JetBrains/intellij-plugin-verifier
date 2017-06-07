package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.utils.BytecodeUtil
import com.jetbrains.pluginverifier.utils.checkClassExistsOrExternal
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TryCatchBlockNode

/**
 * @author Sergey Patrikeev
 */
class MethodTryCatchVerifier : MethodVerifier {
  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    val blocks = method.tryCatchBlocks as List<TryCatchBlockNode>
    for (block in blocks) {
      val catchException = block.type ?: continue
      val descr = BytecodeUtil.extractClassNameFromDescr(catchException) ?: continue
      ctx.checkClassExistsOrExternal(descr, { ctx.fromMethod(clazz, method) })
    }
  }
}
