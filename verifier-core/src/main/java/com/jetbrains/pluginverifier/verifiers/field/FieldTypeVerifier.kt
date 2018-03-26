package com.jetbrains.pluginverifier.verifiers.field

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.checkClassExistsOrExternal
import com.jetbrains.pluginverifier.verifiers.createFieldLocation
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

/**
 * @author Dennis.Ushakov
 */
class FieldTypeVerifier : FieldVerifier {
  override fun verify(clazz: ClassNode, field: FieldNode, ctx: VerificationContext) {
    val className = field.desc.extractClassNameFromDescr() ?: return

    ctx.checkClassExistsOrExternal(className, { createFieldLocation(clazz, field) })
  }
}
