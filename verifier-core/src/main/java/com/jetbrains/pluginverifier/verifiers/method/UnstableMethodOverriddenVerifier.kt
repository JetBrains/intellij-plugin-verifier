package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.results.deprecated.DeprecatedMethodOverridden
import com.jetbrains.pluginverifier.results.experimental.ExperimentalMethodOverridden
import com.jetbrains.pluginverifier.verifiers.*
import com.jetbrains.pluginverifier.verifiers.logic.hierarchy.ClassParentsVisitor
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class UnstableMethodOverriddenVerifier : MethodVerifier {

  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    if (method.isStatic() || method.isPrivate() || method.isConstructor() || method.isClassInitializer()) return

    val superClass = clazz.superName

    if (superClass == null || superClass.startsWith("[")) {
      return
    }

    ClassParentsVisitor(ctx, true).visitClass(
        clazz,
        false,
        onEnter = { parent ->
          checkSuperMethod(ctx, clazz, method, parent)
        }
    )
  }

  private fun checkSuperMethod(
      ctx: VerificationContext,
      clazz: ClassNode,
      method: MethodNode,
      parent: ClassNode
  ): Boolean {
    @Suppress("UNCHECKED_CAST")
    val sameMethod = (parent.methods as List<MethodNode>)
        .firstOrNull { it.name == method.name && it.desc == method.desc }

    if (sameMethod != null) {
      val methodDeprecated = sameMethod.getDeprecationInfo()
      if (methodDeprecated != null) {
        val methodLocation = createMethodLocation(parent, sameMethod)
        ctx.registerDeprecatedUsage(
            DeprecatedMethodOverridden(
                methodLocation,
                createMethodLocation(clazz, method),
                methodDeprecated
            )
        )
      }

      val experimentalApi = sameMethod.isExperimentalApi()
      if (experimentalApi) {
        val methodLocation = createMethodLocation(parent, sameMethod)
        ctx.registerExperimentalApiUsage(
            ExperimentalMethodOverridden(
                methodLocation,
                createMethodLocation(clazz, method)
            )
        )
      }

      if (experimentalApi || methodDeprecated != null) {
        return false
      }
    }
    return true
  }

}