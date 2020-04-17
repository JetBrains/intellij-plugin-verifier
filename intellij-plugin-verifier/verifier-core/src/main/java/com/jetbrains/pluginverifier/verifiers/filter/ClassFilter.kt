/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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