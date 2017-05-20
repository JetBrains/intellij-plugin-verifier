package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode

interface ClassVerifier {
  fun verify(clazz: ClassNode, ctx: VerificationContext)
}
