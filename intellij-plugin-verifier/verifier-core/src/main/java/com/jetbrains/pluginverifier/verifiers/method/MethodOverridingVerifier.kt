/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.toReference
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.ProblemRegistrar
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.MethodResolver
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import com.jetbrains.pluginverifier.warnings.WarningRegistrar

class MethodOverridingVerifier(private val methodOverridingProcessors: List<MethodOverridingProcessor>) : MethodVerifier {

  override fun verify(method: Method, context: VerificationContext) {
    if (method.isStatic || method.isPrivate || method.name == "<init>" || method.name == "<clinit>") return

    val overriddenMethod = MethodResolver().resolveMethod(
      ClassFileWithNoMethodsWrapper(method.containingClassFile),
      method.location.toReference(),
      if (method.containingClassFile.isInterface) Instruction.INVOKE_INTERFACE else Instruction.INVOKE_VIRTUAL,
      method,
      VerificationContextWithSilentProblemRegistrar(context)
    )
    if (overriddenMethod != null && method.containingClassFile.name != overriddenMethod.containingClassFile.name) {
      for (processor in methodOverridingProcessors) {
        processor.processMethodOverriding(method, overriddenMethod, context)
      }
    }
  }
}

private class ClassFileWithNoMethodsWrapper(
  private val classFile: ClassFile
) : ClassFile by classFile {
  override val methods: Sequence<Method> get() = emptySequence()
}

private class VerificationContextWithSilentProblemRegistrar(
  private val delegate: VerificationContext
) : VerificationContext by delegate {
  override val problemRegistrar: ProblemRegistrar = object : ProblemRegistrar {
    override fun registerProblem(problem: CompatibilityProblem) = Unit
  }

  override val warningRegistrar: WarningRegistrar = object : WarningRegistrar {
    override fun registerCompatibilityWarning(warning: CompatibilityWarning) = Unit
  }
}