package com.jetbrains.pluginverifier.verifiers.field

import com.jetbrains.pluginverifier.utils.VerificationContext
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.FieldNode

/**
 * @author Dennis.Ushakov
 */
class FieldTypeVerifier : FieldVerifier {
  override fun verify(clazz: ClassNode, field: FieldNode, ctx: VerificationContext) {
    val className = VerifierUtil.extractClassNameFromDescr(field.desc) ?: return

    VerifierUtil.checkClassExistsOrExternal(className, ctx, { ctx.fromField(clazz, field) })
  }
}
