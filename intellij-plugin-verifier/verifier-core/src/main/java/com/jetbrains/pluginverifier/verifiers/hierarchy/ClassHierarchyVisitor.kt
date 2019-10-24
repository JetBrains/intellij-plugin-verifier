package com.jetbrains.pluginverifier.verifiers.hierarchy

import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy

class ClassHierarchyVisitor(private val visitInterfaces: Boolean) {

  private val visitedClasses = hashSetOf<String>()

  fun visitClassHierarchy(
    currentClass: ClassHierarchy,
    visitSelf: Boolean,
    onEnter: (ClassHierarchy) -> Boolean,
    onExit: (ClassHierarchy) -> Unit = {}
  ) {
    visitedClasses.add(currentClass.name)

    if (visitSelf && !onEnter(currentClass)) {
      return
    }

    val interfaces = if (visitInterfaces) {
      currentClass.superInterfaces
    } else {
      emptyList()
    }

    val superParents = listOfNotNull(currentClass.superClass) + interfaces

    superParents
      .asSequence()
      .filterNot { it.name in visitedClasses }
      .forEach { visitClassHierarchy(it, true, onEnter, onExit) }

    if (visitSelf) {
      onExit(currentClass)
    }
  }


}