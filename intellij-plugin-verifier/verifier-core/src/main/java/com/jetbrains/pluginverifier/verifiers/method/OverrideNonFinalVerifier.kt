package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.results.problems.OverridingFinalMethodProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.hierarchy.ClassParentsVisitor
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class OverrideNonFinalVerifier : MethodVerifier {

  override fun verify(method: Method, context: VerificationContext) {
    if (method.isPrivate || method.name == "<init>" || method.name == "<clinit>") return

    /*
     * According to JVM 8 specification the static methods cannot <i>override</i> the parent methods.
     * They can only <i>hide</i> them. Java compiler prohibits <i>hiding</i> the final static methods of the parent,
     * but Java Virtual Machine (at least the 8-th version) allows to invoke such methods and doesn't complain
     * during the class-file verification
     */
    if (method.isStatic) return

    /*
     * Traverse the super-classes up to the java.lang.Object and check that the verified class
     * doesn't override a final method.
     * Java interfaces are not allowed to have final methods so it works.
     */
    val parentsVisitor = ClassParentsVisitor(false) { subclassFile, superName ->
      context.classResolver.resolveClassChecked(superName, subclassFile, context)
    }
    parentsVisitor.visitClass(method.owner, false, onEnter = { parent ->
      val sameMethod = parent.methods.find { it.name == method.name && it.descriptor == method.descriptor }
      if (sameMethod != null && sameMethod.isFinal) {
        context.problemRegistrar.registerProblem(OverridingFinalMethodProblem(sameMethod.location, method.owner.location))
        false
      } else {
        true
      }
    })
  }

}
