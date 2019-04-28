package com.jetbrains.pluginverifier.verifiers.filter

import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile

/**
 * Implementations of this interface determine whether
 * a class file should be verified with bytecode analysis.
 */
interface ClassFilter {

  /**
   * Determines whether [classFile] must be verified
   */
  fun shouldVerify(classFile: ClassFile): Boolean

}