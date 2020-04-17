/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.problems.InheritFromFinalClassProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked

class InheritFromFinalClassVerifier : ClassVerifier {
  override fun verify(classFile: ClassFile, context: VerificationContext) {
    val superClassName = classFile.superName ?: return
    val superClass = context.classResolver.resolveClassChecked(superClassName, classFile, context)
      ?: return
    if (superClass.isFinal) {
      context.problemRegistrar.registerProblem(InheritFromFinalClassProblem(classFile.location, superClass.location))
    }
  }
}
