package com.jetbrains.pluginverifier.verifiers.hierarchy

import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile

class ClassParentsVisitor(
    private val visitInterfaces: Boolean,
    private val parentResolver: (subclassFile: ClassFile, parentClassName: String) -> ClassFile?
) {

  private val visitedClasses = hashSetOf<String>()

  fun visitClass(
      currentClass: ClassFile,
      visitSelf: Boolean,
      onEnter: (ClassFile) -> Boolean,
      onExit: (ClassFile) -> Unit = {}
  ) {
    visitedClasses.add(currentClass.name)

    if (visitSelf && !onEnter(currentClass)) {
      return
    }

    val interfaces = if (visitInterfaces) {
      currentClass.interfaces
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