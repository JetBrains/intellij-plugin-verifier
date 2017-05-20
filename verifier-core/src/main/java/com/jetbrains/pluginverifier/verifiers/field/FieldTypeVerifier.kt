package com.jetbrains.pluginverifier.verifiers.field

import com.jetbrains.pluginverifier.utils.BytecodeUtil
import com.jetbrains.pluginverifier.utils.checkClassExistsOrExternal
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.FieldNode

/**
 * @author Dennis.Ushakov
 */
class FieldTypeVerifier : FieldVerifier {
  override fun verify(clazz: ClassNode, field: FieldNode, ctx: VerificationContext) {
    val className = BytecodeUtil.extractClassNameFromDescr(field.desc) ?: return

    ctx.checkClassExistsOrExternal(className, { ctx.fromField(clazz, field) })
  }
}
