/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.properties

import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.bytecode.InterpreterAdapter
import com.jetbrains.pluginverifier.verifiers.bytecode.InvokeSpecialInterpreterListener
import com.jetbrains.pluginverifier.verifiers.bytecode.StringValue
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.MethodAsm
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.Analyzer

class EnumPropertyUsageProcessor(private val propertyChecker: PropertyChecker) : ApiUsageProcessor {
  private val enumClassPropertyUsage = EnumClassPropertyUsageAdapter()

  override fun processMethodInvocation(
    methodReference: MethodReference,
    resolvedMethod: Method,
    instructionNode: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ) {
    if (callerMethod !is MethodAsm) return
    val invokeSpecialDetector = InvokeSpecialInterpreterListener()
    Analyzer(InterpreterAdapter(invokeSpecialDetector)).analyze(
      callerMethod.containingClassFile.name,
      callerMethod.asmNode
    )
    enumClassPropertyUsage.resolve(resolvedMethod)?.let { resourceBundledProperty ->
      invokeSpecialDetector.invocations.filter {
        it.methodName == "<init>" && enumClassPropertyUsage.isEnumConstructorDesc(it.desc)
      }.forEach { constructorInvocation ->
        // Drop the following parameters
        //    1) invocation target 2) enum member name 3) enum ordinal value
        // Such parameters are passed to the pseudo-synthetic private enum constructor
        val invocationParameteres = constructorInvocation.values.drop(3)
        invocationParameteres.filterIsInstance<StringValue>().forEach {
          propertyChecker.checkProperty(resourceBundledProperty.bundleName, it.value, context, callerMethod.location)
        }
      }
    }
    return
  }

  fun supports(method: Method): Boolean {
    return enumClassPropertyUsage.supports(method)
  }
}