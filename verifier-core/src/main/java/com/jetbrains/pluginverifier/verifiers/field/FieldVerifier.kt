package com.jetbrains.pluginverifier.verifiers.field

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

/**
 * @author Dennis.Ushakov
 */
interface FieldVerifier {
  fun verify(clazz: ClassNode, field: FieldNode, resolver: Resolver, ctx: VContext)
}
