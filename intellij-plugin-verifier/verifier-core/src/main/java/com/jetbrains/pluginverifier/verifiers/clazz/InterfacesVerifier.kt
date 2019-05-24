package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.problems.SuperInterfaceBecameClassProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile

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
