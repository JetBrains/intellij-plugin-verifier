package com.jetbrains.pluginverifier.verifiers

import org.objectweb.asm.tree.ClassNode

class ClassParentsVisitor(private val context: VerificationContext,
                          private val visitInterfaces: Boolean) {

  private val visitedClasses: MutableSet<String> = hashSetOf()

  fun visitClassAndParents(currentClass: ClassNode, classProcessor: (ClassNode) -> Boolean) {
    visitedClasses.add(currentClass.name)

    if (!classProcessor(currentClass)) {
      return
    }

    val superName: String = currentClass.superName ?: "java/lang/Object"

    val superClassName = listOf(superName)
    @Suppress("UNCHECKED_CAST")
    val interfaces = if (visitInterfaces) {
      currentClass.interfaces as List<String>
    } else {
      emptyList()
    }
    (superClassName + interfaces).forEach { clsName ->
      if (clsName !in visitedClasses) {
        val parentNode = context.resolveClassOrProblem(clsName, currentClass, { context.fromClass(currentClass) })
        if (parentNode != null) {
          visitClassAndParents(parentNode, classProcessor)
        }
      }
    }
  }


}