package com.jetbrains.pluginverifier.verifiers.logic.hierarchy

import org.objectweb.asm.tree.ClassNode

class ClassParentsVisitor(
    private val visitInterfaces: Boolean,
    private val parentResolver: (subclassNode: ClassNode, parentClassName: String) -> ClassNode?
) {

  private val visitedClasses = hashSetOf<String>()

  fun visitClass(
      currentClass: ClassNode,
      visitSelf: Boolean,
      onEnter: (ClassNode) -> Boolean,
      onExit: (ClassNode) -> Unit = {}
  ) {
    visitedClasses.add(currentClass.name)

    if (visitSelf && !onEnter(currentClass)) {
      return
    }

    @Suppress("UNCHECKED_CAST")
    val interfaces = if (visitInterfaces) {
      currentClass.interfaces as List<String>
    } else {
      emptyList()
    }

    val superParents = listOfNotNull(currentClass.superName) + interfaces

    for (superParent in superParents) {
      if (superParent !in visitedClasses) {
        val superNode = parentResolver(currentClass, superParent)
        if (superNode != null) {
          visitClass(superNode, true, onEnter, onExit)
        }
      }
    }

    if (visitSelf) {
      onExit(currentClass)
    }
  }

}