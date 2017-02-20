package com.jetbrains.pluginverifier.verifiers.field

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.FieldNode

/**
 * @author Dennis.Ushakov
 */
class FieldTypeVerifier : FieldVerifier {
  override fun verify(clazz: ClassNode, field: FieldNode, resolver: Resolver, ctx: VContext) {
    val className = VerifierUtil.extractClassNameFromDescr(field.desc) ?: return

    VerifierUtil.checkClassExistsOrExternal(resolver, className, ctx, { ctx.fromField(clazz, field) })
  }
}
