package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.results.deprecated.DeprecatedMethodUsage
import com.jetbrains.pluginverifier.results.problems.OverridingFinalMethodProblem
import com.jetbrains.pluginverifier.verifiers.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode


/**
 * @author Dennis.Ushakov
 */
class OverrideNonFinalVerifier : MethodVerifier {


  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    if (method.isPrivate()) return

    /*
    According to JVM 8 specification the static methods cannot <i>override</i> the parent methods.
    They can only <i>hide</i> them. Java compiler prohibits <i>hiding</i> the final static methods of the parent,
    but Java Virtual Machine (at least the 8-th version) allows to invoke such methods and doesn't complain
    during the class-file verification
     */
    if (method.isStatic()) return

    val superClass = clazz.superName

    if (superClass == null || superClass.startsWith("[") || ctx.isExternalClass(superClass)) {
      return
    }

    var parent: ClassNode? = ctx.resolveClassOrProblem(superClass, clazz, { ctx.fromMethod(clazz, method) }) ?: return

    /**
     * Traverse the super-classes up to the java.lang.Object and check that the verified class
     * doesn't override a final method.
     * Java interfaces are not allowed to have final methods so it works.
     */
    while (parent != null) {
      val sameMethod = (parent.methods as List<MethodNode>).firstOrNull { it.name == method.name && it.desc == method.desc }
      if (sameMethod != null) {
        val methodLocation = ctx.fromMethod(parent, sameMethod)
        if (sameMethod.isDeprecated()) {
          val usageLocation = ctx.fromMethod(clazz, method)
          ctx.registerDeprecatedUsage(DeprecatedMethodUsage(methodLocation, usageLocation))
        }
        if (sameMethod.isFinal()) {
          ctx.registerProblem(OverridingFinalMethodProblem(methodLocation, ctx.fromClass(clazz)))
          return
        }
      }
      val superName = parent.superName ?: break
      val superNode = ctx.resolveClassOrProblem(superName, parent, { ctx.fromClass(parent!!) }) ?: break
      parent = superNode
    }
  }

}
