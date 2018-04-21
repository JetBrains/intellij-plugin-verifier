package com.jetbrains.pluginverifier.verifiers.logic.hierarchy

import com.jetbrains.pluginverifier.misc.singletonOrEmpty
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.createClassLocation
import com.jetbrains.pluginverifier.verifiers.resolveClassOrProblem
import org.objectweb.asm.tree.ClassNode

class ClassParentsVisitor(private val context: VerificationContext,
                          private val visitInterfaces: Boolean) {

  private val visitedClasses = hashSetOf<String>()

  fun visitClass(currentClass: ClassNode,
                 visitSelf: Boolean,
                 onEnter: (ClassNode) -> Boolean,
                 onExit: (ClassNode) -> Unit = {}) {
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

    val superParents = currentClass.superName.singletonOrEmpty() + interfaces

    for (superParent in superParents) {
      if (superParent !in visitedClasses) {
        val superNode = context.resolveClassOrProblem(superParent, currentClass, { currentClass.createClassLocation() })
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