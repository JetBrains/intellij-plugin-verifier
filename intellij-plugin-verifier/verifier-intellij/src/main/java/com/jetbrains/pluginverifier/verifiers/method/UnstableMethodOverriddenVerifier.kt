package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.results.deprecated.DeprecatedMethodOverridden
import com.jetbrains.pluginverifier.results.experimental.ExperimentalMethodOverridden
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.getDeprecationInfo
import com.jetbrains.pluginverifier.verifiers.hierarchy.ClassParentsVisitor
import com.jetbrains.pluginverifier.verifiers.isExperimentalApi
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class UnstableMethodOverriddenVerifier : MethodVerifier {

  override fun verify(method: Method, context: VerificationContext) {
    if (method.isStatic || method.isPrivate || method.name == "<init>" || method.name == "<clinit>") return

    val classParentsVisitor = ClassParentsVisitor(true) { subclassNode, superName ->
      context.classResolver.resolveClassChecked(superName, subclassNode, context)
    }
    classParentsVisitor.visitClass(
        method.owner,
        false,
        onEnter = { parent ->
          checkSuperMethod(context, method, parent)
        }
    )
  }

  private fun checkSuperMethod(context: VerificationContext, method: Method, parent: ClassFile): Boolean {
    val sameMethod = parent.methods.find { it.name == method.name && it.descriptor == method.descriptor }

    if (sameMethod != null) {
      val methodDeprecated = sameMethod.getDeprecationInfo()
      if (methodDeprecated != null) {
        context.deprecatedApiRegistrar.registerDeprecatedUsage(
            DeprecatedMethodOverridden(
                sameMethod.location,
                method.location,
                methodDeprecated
            )
        )
      }

      val experimentalApi = sameMethod.isExperimentalApi()
      if (experimentalApi) {
        context.experimentalApiRegistrar.registerExperimentalApiUsage(
            ExperimentalMethodOverridden(
                sameMethod.location,
                method.location
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