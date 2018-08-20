package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.problems.SuperClassBecameInterfaceProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.createClassLocation
import com.jetbrains.pluginverifier.verifiers.isInterface
import com.jetbrains.pluginverifier.verifiers.logic.CommonClassNames
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.tree.ClassNode

/**
 * Check that superclass exists and is indeed a class (not interface).
 *
 * @author Dennis.Ushakov
 */
class SuperClassVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, ctx: VerificationContext) {
    val superClassName = clazz.superName ?: CommonClassNames.JAVA_LANG_OBJECT
    val superNode = ctx.resolveClassOrProblem(superClassName, clazz) { clazz.createClassLocation() } ?: return
    //If the class or interface named as the direct superclass of C is in fact an interface, loading throws an IncompatibleClassChangeError.
    if (superNode.isInterface()) {
      ctx.registerProblem(SuperClassBecameInterfaceProblem(clazz.createClassLocation(), superNode.createClassLocation()))
    }
  }
}
