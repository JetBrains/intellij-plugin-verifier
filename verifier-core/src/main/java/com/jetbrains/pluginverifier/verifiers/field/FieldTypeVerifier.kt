package com.jetbrains.pluginverifier.verifiers.field

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

/**
 * @author Dennis.Ushakov
 */
class FieldTypeVerifier : FieldVerifier {
  override fun verify(clazz: ClassNode, field: FieldNode, resolver: Resolver, ctx: VContext) {
    val className = VerifierUtil.extractClassNameFromDescr(field.desc) ?: return

    VerifierUtil.checkClassExistsOrExternal(resolver, className, ctx, { ProblemLocation.fromField(clazz.name, field.name, field.desc) })
  }
}
