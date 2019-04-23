package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.results.problems.OverridingFinalMethodProblem
import com.jetbrains.pluginverifier.verifiers.*
import com.jetbrains.pluginverifier.verifiers.logic.hierarchy.ClassParentsVisitor
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class OverrideNonFinalVerifier : MethodVerifier {


  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    if (method.isPrivate() || method.isConstructor() || method.isClassInitializer()) return

    /*
    According to JVM 8 specification the static methods cannot <i>override</i> the parent methods.
    They can only <i>hide</i> them. Java compiler prohibits <i>hiding</i> the final static methods of the parent,
    but Java Virtual Machine (at least the 8-th version) allows to invoke such methods and doesn't complain
    during the class-file verification
     */
    if (method.isStatic()) return

    val superClass = clazz.superName

    if (superClass == null || superClass.startsWith("[") || ctx.classResolver.isExternalClass(superClass)) {
      return
    }

    /**
     * Traverse the super-classes up to the java.lang.Object and check that the verified class
     * doesn't override a final method.
     * Java interfaces are not allowed to have final methods so it works.
     */
    val parentsVisitor = ClassParentsVisitor(false) { subclassNode, superName ->
      ctx.resolveClassOrProblem(superName, subclassNode) { subclassNode.createClassLocation() }
    }
    parentsVisitor.visitClass(clazz, false, onEnter = { parent ->
      val sameMethod = parent.getMethods().orEmpty().find { it.name == method.name && it.desc == method.desc }
      if (sameMethod != null && sameMethod.isFinal()) {
        val methodLocation = createMethodLocation(parent, sameMethod)
        ctx.registerProblem(OverridingFinalMethodProblem(methodLocation, clazz.createClassLocation()))
        false
      } else {
        true
      }
    })
  }

}
