/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.method.KotlinMethods.isKotlinDefaultMethod
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked
import org.objectweb.asm.Type

class MethodArgumentTypesVerifier : MethodVerifier {
  override fun verify(method: Method, context: VerificationContext) {
    if (method.isKotlinDefaultMethod()) {
      return
    }

    val methodType = Type.getType(method.descriptor)
    val argumentTypes = methodType.argumentTypes
    for (type in argumentTypes) {
      val className = type.descriptor.extractClassNameFromDescriptor() ?: continue
      context.classResolver.resolveClassChecked(className, method, context, ClassUsageType.METHOD_PARAMETER)
    }
  }
}
