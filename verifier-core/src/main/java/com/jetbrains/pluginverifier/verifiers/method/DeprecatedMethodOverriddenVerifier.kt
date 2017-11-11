package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.results.deprecated.DeprecatedMethodOverridden
import com.jetbrains.pluginverifier.verifiers.*
import com.jetbrains.pluginverifier.verifiers.logic.hierarchy.ClassParentsVisitor
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class DeprecatedMethodOverriddenVerifier : MethodVerifier {

  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    if (method.isStatic() || method.isPrivate() || method.isConstructor() || method.isClassInitializer()) return

    val superClass = clazz.superName

    if (superClass == null || superClass.startsWith("[")) {
      return
    }

    ClassParentsVisitor(ctx, true).visitClass(clazz, false, onEnter = { parent ->
      val sameMethod = (parent.methods as List<MethodNode>).firstOrNull { it.name == method.name && it.desc == method.desc }
      if (sameMethod != null && sameMethod.isDeprecated()) {
        val methodLocation = ctx.fromMethod(parent, sameMethod)
        ctx.registerDeprecatedUsage(DeprecatedMethodOverridden(methodLocation, ctx.fromMethod(clazz, method)))
        false
      } else {
        true
      }
    })
  }

}