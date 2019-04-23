package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.problems.MethodNotImplementedProblem
import com.jetbrains.pluginverifier.verifiers.*
import com.jetbrains.pluginverifier.verifiers.logic.hierarchy.ClassParentsVisitor
import org.objectweb.asm.tree.ClassNode

class AbstractMethodVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, ctx: VerificationContext) {
    if (clazz.isAbstract() || clazz.isInterface()) return

    val abstractMethods = hashMapOf<MethodSignature, MethodLocation>()
    val implementedMethods = hashMapOf<MethodSignature, MethodLocation>()
    var hasUnresolvedParents = false

    val parentsVisitor = ClassParentsVisitor(true) { subclassNode, superName ->
      val classNode = ctx.resolveClassOrProblem(superName, subclassNode) { subclassNode.createClassLocation() }
      hasUnresolvedParents = hasUnresolvedParents || classNode == null
      classNode
    }

    parentsVisitor.visitClass(clazz, true, onEnter = { parent ->
      parent.getMethods().orEmpty().forEach { method ->
        if (!method.isPrivate() && !method.isStatic()) {
          val methodLocation = createMethodLocation(parent, method)
          val methodSignature = MethodSignature(method.name, method.desc)
          if (method.isAbstract()) {
            abstractMethods[methodSignature] = methodLocation
          } else {
            implementedMethods[methodSignature] = methodLocation
          }
        }
      }
      true
    })

    if (!hasUnresolvedParents) {
      val currentClass = clazz.createClassLocation()
      (abstractMethods.keys - implementedMethods.keys).forEach { method ->
        val abstractMethod = abstractMethods[method]!!
        ctx.registerProblem(MethodNotImplementedProblem(abstractMethod, currentClass))
      }
    }
  }

  private data class MethodSignature(val name: String, val descriptor: String)

}