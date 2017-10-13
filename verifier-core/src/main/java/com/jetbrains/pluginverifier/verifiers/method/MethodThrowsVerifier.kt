package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.BytecodeUtil
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.checkClassExistsOrExternal
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
class MethodThrowsVerifier : MethodVerifier {
  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    val exceptions = method.exceptions as List<String>
    for (exception in exceptions) {
      val descr = BytecodeUtil.extractClassNameFromDescr(exception) ?: continue
      ctx.checkClassExistsOrExternal(descr, clazz, { ctx.fromMethod(clazz, method) })
    }
  }
}
