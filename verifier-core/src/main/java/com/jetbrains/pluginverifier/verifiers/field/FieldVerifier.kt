package com.jetbrains.pluginverifier.verifiers.field

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

interface FieldVerifier {
  fun verify(clazz: ClassNode, field: FieldNode, ctx: VerificationContext)
}
