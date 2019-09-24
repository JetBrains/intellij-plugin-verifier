package com.jetbrains.plugin.structure.classes.resolvers

import org.objectweb.asm.tree.ClassNode

sealed class ResolutionResult {

  object NotFound : ResolutionResult()

  data class InvalidClassFile(val message: String) : ResolutionResult()

  data class FailedToReadClassFile(val reason: String) : ResolutionResult()

  data class Found(val classNode: ClassNode, val fileOrigin: FileOrigin) : ResolutionResult()
}
