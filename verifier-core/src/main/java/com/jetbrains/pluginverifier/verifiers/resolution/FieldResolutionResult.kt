package com.jetbrains.pluginverifier.verifiers.resolution

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

sealed class FieldResolutionResult {
  object Abort : FieldResolutionResult()

  object NotFound : FieldResolutionResult()

  data class Found(val definingClass: ClassNode, val fieldNode: FieldNode) : FieldResolutionResult()
}