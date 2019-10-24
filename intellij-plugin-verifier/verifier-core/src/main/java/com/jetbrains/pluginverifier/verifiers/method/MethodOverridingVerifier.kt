package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.hierarchy.ClassParentsVisitor
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked

class MethodOverridingVerifier(private val methodOverridingProcessors: List<MethodOverridingProcessor>) : MethodVerifier {

  override fun verify(method: Method, context: VerificationContext) {
    if (method.isStatic || method.isPrivate || method.name == "<init>" || method.name == "<clinit>") return

    val classParentsVisitor = ClassParentsVisitor(true) { subclassNode, superName ->
      context.classResolver.resolveClassChecked(superName, subclassNode, context)
    }
    classParentsVisitor.visitClass(
      method.containingClassFile,
      false,
      onEnter = { parent ->
        checkSuperMethod(method, parent, context)
        true
      }
    )
  }

  private fun checkSuperMethod(method: Method, parent: ClassFile, context: VerificationContext) {
    val overriddenMethod = parent.methods.find { it.name == method.name && it.descriptor == method.descriptor }
    overriddenMethod ?: return
    for (processor in methodOverridingProcessors) {
      processor.processMethodOverriding(method, overriddenMethod, context)
    }
  }

}