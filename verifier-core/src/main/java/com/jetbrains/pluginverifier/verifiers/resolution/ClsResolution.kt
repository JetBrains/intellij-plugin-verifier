package com.jetbrains.pluginverifier.verifiers.resolution

import org.objectweb.asm.tree.ClassNode

/**
 * Possible outcomes of a class [resolution] [ClsResolver]
 */
sealed class ClsResolution {

  /**
   * Class by specified name is not in the [ClsResolver]
   */
  object NotFound : ClsResolution()

  /**
   * Class by specified name is considered "external"
   * meaning that its bytecode is not available
   * but the [ClsResolver] knows about its existence.
   */
  object ExternalClass : ClsResolution()

  /**
   * Class by specified name is available but has invalid bytecode.
   */
  data class InvalidClassFile(val asmError: String) : ClsResolution()

  /**
   * The [ClsResolver] failed to read bytecode of class by specified name
   * from its origin location.
   */
  data class FailedToReadClassFile(val reason: String) : ClsResolution()

  /**
   * Class by specified name is successfully resolved.
   */
  data class Found(val node: ClassNode) : ClsResolution()
}