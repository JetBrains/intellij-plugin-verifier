package com.jetbrains.pluginverifier.verifiers.field

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.utils.VerificationContext
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.FieldNode

/**
 * @author Dennis.Ushakov
 */
interface FieldVerifier {
  fun verify(clazz: ClassNode, field: FieldNode, resolver: Resolver, ctx: VerificationContext)
}
