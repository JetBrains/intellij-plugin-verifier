package com.jetbrains.pluginverifier.verifiers.method

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.problems.OverridingFinalMethodProblem
import com.jetbrains.pluginverifier.utils.LocationUtils
import com.jetbrains.pluginverifier.utils.ResolverUtil
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode


/**
 * @author Dennis.Ushakov
 */
class OverrideNonFinalVerifier : MethodVerifier {

  //TODO: add the following use-case:
  // static or private method overrides instance method (overriding abstract method is already processed in AbstractVerifier)

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

    val superNode = VerifierUtil.findClass(resolver, superClass, ctx)
    if (superNode == null) {
      ctx.registerProblem(ClassNotFoundProblem(superClass), ProblemLocation.fromMethod(clazz.name, method))
      return
    }

    val superMethod = ResolverUtil.findMethod(resolver, superNode, method.name, method.desc, ctx) ?: return

    val classNode = superMethod.classNode
    val methodNode = superMethod.methodNode

    if (VerifierUtil.isFinal(methodNode) && !VerifierUtil.isAbstract(methodNode)) {
      ctx.registerProblem(OverridingFinalMethodProblem(LocationUtils.getMethodLocation(classNode, methodNode)), ProblemLocation.fromMethod(clazz.name, method))
    }
  }
}
