package com.jetbrains.pluginverifier.verifiers.filter

import org.objectweb.asm.tree.ClassNode

/**
 * Implementations of this interface determine whether
 * a class file should be verified with bytecode analysis.
 */
interface VerificationFilter {

  /**
   * Determines whether [classNode] must be verified
   */
  fun shouldVerify(classNode: ClassNode): Boolean

}