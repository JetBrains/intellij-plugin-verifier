package com.jetbrains.pluginverifier.verifiers.field

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.createFieldLocation
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

/**
 * @author Dennis.Ushakov
 */
class FieldTypeVerifier : FieldVerifier {
  override fun verify(clazz: ClassNode, field: FieldNode, ctx: VerificationContext) {
    val className = field.desc.extractClassNameFromDescr() ?: return

    ctx.resolveClassOrProblem(className, clazz) { createFieldLocation(clazz, field) }
  }
}
