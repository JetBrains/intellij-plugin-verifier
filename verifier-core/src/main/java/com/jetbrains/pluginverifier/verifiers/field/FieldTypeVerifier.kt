package com.jetbrains.pluginverifier.verifiers.field

import com.jetbrains.pluginverifier.verifiers.BytecodeUtil
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.checkClassExistsOrExternal
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

/**
 * @author Dennis.Ushakov
 */
class FieldTypeVerifier : FieldVerifier {
  override fun verify(clazz: ClassNode, field: FieldNode, ctx: VerificationContext) {
    val className = BytecodeUtil.extractClassNameFromDescr(field.desc) ?: return

    ctx.checkClassExistsOrExternal(className, clazz, { ctx.fromField(clazz, field) })
  }
}
