package com.jetbrains.pluginverifier.verifiers.method

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.problems.OverridingFinalMethodProblem
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode


/**
 * @author Dennis.Ushakov
 */
class OverrideNonFinalVerifier : MethodVerifier {


  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, resolver: Resolver, ctx: VContext) {
    if (VerifierUtil.isPrivate(method)) return

    /*
    According to JVM 8 specification the static methods cannot <i>override</i> the parent methods.
    They can only <i>hide</i> them. Java compiler prohibits <i>hiding</i> the final static methods of the parent,
    but Java Virtual Machine (at least the 8-th version) allows to invoke such methods and doesn't complain
    during the class-file verification
     */
    if (VerifierUtil.isStatic(method)) return

    val superClass = clazz.superName

    if (superClass == null || superClass.startsWith("[") || ctx.verifierOptions.isExternalClass(superClass)) {
      return
    }

    var parent: ClassNode? = VerifierUtil.resolveClassOrProblem(resolver, superClass, clazz, ctx, { ctx.fromMethod(clazz, method) }) ?: return

    while (parent != null) {
      val sameMethod = (parent.methods as List<MethodNode>).firstOrNull { it.name == method.name && it.desc == method.desc }
      if (sameMethod != null && VerifierUtil.isFinal(sameMethod)) {
        val methodLocation = ctx.fromMethod(parent, sameMethod)
        val thisClass = ctx.fromClass(clazz)
        ctx.registerProblem(OverridingFinalMethodProblem(methodLocation, thisClass))
        return
      }
      val superName = parent.superName ?: break
      val superNode = VerifierUtil.resolveClassOrProblem(resolver, superName, parent, ctx, { ctx.fromClass(parent!!) }) ?: break
      parent = superNode
    }
  }

}
