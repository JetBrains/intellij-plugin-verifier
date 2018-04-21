package com.jetbrains.pluginverifier.verifiers.resolution

import org.objectweb.asm.tree.ClassNode

sealed class ClsResolution {
  object NotFound : ClsResolution()

  object ExternalClass : ClsResolution()

  data class InvalidClassFile(val asmError: String) : ClsResolution()

  data class FailedToReadClassFile(val reason: String) : ClsResolution()

  data class Found(val node: ClassNode) : ClsResolution()
}