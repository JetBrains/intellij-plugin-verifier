/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.method.KotlinMethods.isKotlinDefaultImpl
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked

class MethodLocalVarsVerifier : MethodVerifier {
  override fun verify(method: Method, context: VerificationContext) {
    if (method.isKotlinDefaultImpl()) {
      return
    }

    for (variable in method.localVariables) {
      val className = variable.desc.extractClassNameFromDescriptor() ?: continue
      context.classResolver.resolveClassChecked(className, method, context)
    }
  }
}
