package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author Dennis.Ushakov
 */
interface MethodVerifier {
  fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext)
}
