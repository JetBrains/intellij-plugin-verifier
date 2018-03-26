package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.problems.InheritFromFinalClassProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.createClassLocation
import com.jetbrains.pluginverifier.verifiers.isFinal
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.tree.ClassNode

class InheritFromFinalClassVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, ctx: VerificationContext) {
    val superClassName = clazz.superName ?: return
    val supClass = ctx.resolveClassOrProblem(superClassName, clazz, { clazz.createClassLocation() }) ?: return
    if (supClass.isFinal()) {
      val child = clazz.createClassLocation()
      val finalClass = supClass.createClassLocation()
      ctx.registerProblem(InheritFromFinalClassProblem(child, finalClass))
    }
  }
}
