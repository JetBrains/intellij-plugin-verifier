package com.jetbrains.pluginverifier.verifiers.field

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.FieldNode

/**
 * @author Dennis.Ushakov
 */
interface FieldVerifier {
  fun verify(clazz: ClassNode, field: FieldNode, ctx: VerificationContext)
}
