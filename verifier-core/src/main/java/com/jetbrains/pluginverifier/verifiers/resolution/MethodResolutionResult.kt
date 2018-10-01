package com.jetbrains.pluginverifier.verifiers.resolution

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

sealed class MethodResolutionResult {
  object Abort : MethodResolutionResult()

  object NotFound : MethodResolutionResult()

  data class Found(val definingClass: ClassNode, val methodNode: MethodNode) : MethodResolutionResult()
}