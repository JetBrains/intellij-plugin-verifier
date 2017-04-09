package com.jetbrains.pluginverifier.verifiers.clazz

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.utils.VerificationContext
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode

interface ClassVerifier {
  fun verify(clazz: ClassNode, resolver: Resolver, ctx: VerificationContext)
}
