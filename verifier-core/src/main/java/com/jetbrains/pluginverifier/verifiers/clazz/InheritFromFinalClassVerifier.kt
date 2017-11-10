package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.problems.InheritFromFinalClassProblem
import com.jetbrains.pluginverifier.verifiers.*
import org.objectweb.asm.tree.ClassNode

class InheritFromFinalClassVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, ctx: VerificationContext) {
    val superClassName = clazz.superName ?: ClassParentsVisitor.JAVA_LANG_OBJECT
    val supClass = ctx.resolveClassOrProblem(superClassName, clazz, { ctx.fromClass(clazz) }) ?: return
    if (supClass.isFinal()) {
      val child = ctx.fromClass(clazz)
      val finalClass = ctx.fromClass(supClass)
      ctx.registerProblem(InheritFromFinalClassProblem(child, finalClass))
    }
  }
}
