/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.problems.SuperClassBecameInterfaceProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked

/**
 * Check that superclass exists and is indeed a class (not interface).
 * If the class or interface named as the direct superclass of C is in fact an interface,
 * class loading throws an IncompatibleClassChangeError.
 */
class SuperClassVerifier : ClassVerifier {
  override fun verify(classFile: ClassFile, context: VerificationContext) {
    val superClassName = classFile.superName ?: "java/lang/Object"
    val superClass = context.classResolver.resolveClassChecked(superClassName, classFile, context) ?: return

    if (superClass.isInterface) {
      context.problemRegistrar.registerProblem(SuperClassBecameInterfaceProblem(classFile.location, superClass.location))
    }
  }
}
