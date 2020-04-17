/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.problems.SuperInterfaceBecameClassProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked

/**
 * Check that all explicitly defined interfaces exist and are indeed interfaces (not classes).
 * If any of the classes or interfaces named as direct superinterfaces of C is not in fact an interface,
 * class loading throws an IncompatibleClassChangeError.
 */
class InterfacesVerifier : ClassVerifier {
  override fun verify(classFile: ClassFile, context: VerificationContext) {
    classFile.interfaces
      .mapNotNull { context.classResolver.resolveClassChecked(it, classFile, context) }
      .filterNot { it.isInterface }
      .forEach { context.problemRegistrar.registerProblem(SuperInterfaceBecameClassProblem(classFile.location, it.location)) }
  }
}
