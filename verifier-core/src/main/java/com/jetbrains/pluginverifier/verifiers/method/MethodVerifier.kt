package com.jetbrains.pluginverifier.verifiers.method

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.utils.VerificationContext
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode

/**
 * @author Dennis.Ushakov
 */
interface MethodVerifier {
  fun verify(clazz: ClassNode, method: MethodNode, resolver: Resolver, ctx: VerificationContext)
}
