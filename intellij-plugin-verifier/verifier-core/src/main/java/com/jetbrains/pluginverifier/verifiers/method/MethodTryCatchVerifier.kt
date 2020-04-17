/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked

class MethodTryCatchVerifier : MethodVerifier {
  override fun verify(method: Method, context: VerificationContext) {
    for (block in method.tryCatchBlocks) {
      val catchException = block.type ?: continue
      val className = catchException.extractClassNameFromDescriptor() ?: continue
      context.classResolver.resolveClassChecked(className, method, context)
    }
  }
}
