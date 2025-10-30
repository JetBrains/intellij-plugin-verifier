/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.properties

import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.CodeAnalysis
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.getAnnotationValue
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

class PropertyUsageProcessor(private val propertyChecker: PropertyChecker = DefaultPropertyChecker) : ApiUsageProcessor {

  private val enumPropertyUsageProcessor = EnumPropertyUsageProcessor(propertyChecker)

  override fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    instructionNode: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) {
    if (enumPropertyUsageProcessor.supports(resolvedMethod)) {
      return enumPropertyUsageProcessor.processMethodInvocation(methodReference, resolvedMethod, instructionNode, callerMethod, context)
    }

    val methodParameters = resolvedMethod.methodParameters
    if (methodParameters.any { it.name.contains("default", true) }) {
      //Some resource bundle methods provide default value parameter, which is used if such property is not available in the bundle.
      return
    }
    for ((parameterIndex, methodParameter) in methodParameters.withIndex()) {
      val propertyKeyAnnotation = methodParameter.annotations.findAnnotation("org/jetbrains/annotations/PropertyKey")
        ?: continue

      val resourceBundleName = propertyKeyAnnotation.getAnnotationValue("resourceBundle") as? String ?: continue

      val instructions = callerMethod.instructions
      val instructionIndex = instructions.indexOf(instructionNode)

      val onStackIndex = methodParameters.size - 1 - parameterIndex
      val propertyKey = CodeAnalysis().evaluateConstantString(callerMethod, instructionIndex, onStackIndex)

      if (propertyKey != null) {
        propertyChecker.checkProperty(resourceBundleName, propertyKey, context, callerMethod.location)
      }
    }
  }
}

