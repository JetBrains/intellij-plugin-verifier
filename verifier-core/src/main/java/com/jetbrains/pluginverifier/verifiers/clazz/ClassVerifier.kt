package com.jetbrains.pluginverifier.verifiers.clazz

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import org.objectweb.asm.tree.ClassNode

interface ClassVerifier {
  fun verify(clazz: ClassNode, resolver: Resolver, ctx: VContext)
}
