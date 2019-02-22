package com.jetbrains.pluginverifier.verifiers.resolution

import org.objectweb.asm.tree.ClassNode

/**
 * Possible outcomes of a class resolution.
 */
sealed class ClassResolution {

  /**
   * Class by specified name is not in the [ClassResolver]
   */
  object NotFound : ClassResolution()

  /**
   * Class by specified name is considered "external"
   * meaning that its bytecode is not available
   * but the [ClassResolver] knows about its existence.
   */
  object ExternalClass : ClassResolution()

  /**
   * Class by specified name is available but has invalid bytecode.
   */
  data class InvalidClassFile(val asmError: String) : ClassResolution()

  /**
   * The [ClassResolver] failed to read bytecode of class by specified name
   * from its origin location.
   */
  data class FailedToReadClassFile(val reason: String) : ClassResolution()

  /**
   * Class by specified name is successfully resolved.
   */
  data class Found(val node: ClassNode) : ClassResolution()
}