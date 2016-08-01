package com.jetbrains.pluginverifier.verifiers.method

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author Dennis.Ushakov
 */
interface MethodVerifier {
  fun verify(clazz: ClassNode, method: MethodNode, resolver: Resolver, ctx: VContext)
}
