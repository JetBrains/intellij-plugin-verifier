package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.problems.SuperInterfaceBecameClassProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.createClassLocation
import com.jetbrains.pluginverifier.verifiers.isInterface
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.tree.ClassNode

/**
 * Check that all explicitly defined interfaces exist and are indeed interfaces (not classes).
 */
class InterfacesVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, ctx: VerificationContext) {
    //If any of the classes or interfaces named as direct superinterfaces of C is not in fact an interface, loading throws an IncompatibleClassChangeError.
    @Suppress("UNCHECKED_CAST")
    (clazz.interfaces as List<String>)
        .mapNotNull { ctx.resolveClassOrProblem(it, clazz) { clazz.createClassLocation() } }
        .filterNot { it.isInterface() }
        .forEach { ctx.registerProblem(SuperInterfaceBecameClassProblem(clazz.createClassLocation(), it.createClassLocation())) }
  }
}
