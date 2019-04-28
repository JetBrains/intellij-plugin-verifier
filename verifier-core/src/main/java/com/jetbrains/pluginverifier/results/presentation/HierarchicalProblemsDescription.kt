package com.jetbrains.pluginverifier.results.presentation

import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.verifiers.hierarchy.ClassHierarchyVisitor

object HierarchicalProblemsDescription {
  private fun findCandidateSuperClassesAndInterfaces(ownerHierarchy: ClassHierarchy): Pair<Set<String>, Set<String>> {
    val superClasses = hashSetOf<String>()
    val superInterfaces = hashSetOf<String>()
    ClassHierarchyVisitor(true).visitClassHierarchy(ownerHierarchy, false, onEnter = { parent ->
      if (!parent.name.startsWith("java/")) {
        if (parent.isInterface) {
          superInterfaces.add(parent.name)
        } else {
          superClasses.add(parent.name)
        }
      }
      true
    })
    return superClasses to superInterfaces
  }

  fun presentableElementMightHaveBeenDeclaredInSuperTypes(
      elementType: String,
      ownerHierarchy: ClassHierarchy,
      canBeDeclaredInSuperClass: Boolean,
      canBeDeclaredInSuperInterface: Boolean
  ): String {
    val (allSuperClasses, allSuperInterfaces) = findCandidateSuperClassesAndInterfaces(ownerHierarchy)

    val superClasses = if (canBeDeclaredInSuperClass) allSuperClasses else emptySet()
    val superInterfaces = if (canBeDeclaredInSuperInterface) allSuperInterfaces else emptySet()

    return if (superClasses.isEmpty() && superInterfaces.isEmpty()) {
      ""
    } else buildString {
      append(" The $elementType might have been declared ")
      if (superClasses.isNotEmpty()) {
        append("in the super " + "class".pluralize(superClasses.size))
        append(" (")
        append(superClasses.sorted().joinToString(transform = toFullJavaClassName))
        append(")")
      }
      if (superInterfaces.isNotEmpty()) {
        if (superClasses.isNotEmpty()) {
          append(" or ")
        }
        append("in the super " + "interface".pluralize(superInterfaces.size))
        append(" (")
        append(superInterfaces.sorted().joinToString(transform = toFullJavaClassName))
        append(")")
      }
    }
  }
}