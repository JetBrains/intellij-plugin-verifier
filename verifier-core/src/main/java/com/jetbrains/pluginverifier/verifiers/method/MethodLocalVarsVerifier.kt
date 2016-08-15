package com.jetbrains.pluginverifier.verifiers.method

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LocalVariableNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
class MethodLocalVarsVerifier : MethodVerifier {
  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, resolver: Resolver, ctx: VContext) {
    if (method.localVariables != null) {
      val localVariables = method.localVariables as List<LocalVariableNode>
      for (variable in localVariables) {
        val descr = VerifierUtil.extractClassNameFromDescr(variable.desc) ?: continue
        VerifierUtil.checkClassExistsOrExternal(resolver, descr, ctx, { ProblemLocation.fromMethod(clazz.name, method) })
      }
    }


  }
}
