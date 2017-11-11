package com.jetbrains.pluginverifier.results.presentation

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.verifiers.logic.hierarchy.ClassHierarchyVisitor

object HierarchicalProblemsDescription {
  private fun findIdeSuperClassesAndInterfaces(ownerHierarchy: ClassHierarchy): Pair<Set<String>, Set<String>> {
    val ideSuperClasses = hashSetOf<String>()
    val ideSuperInterfaces = hashSetOf<String>()
    ClassHierarchyVisitor(true).visitClassHierarchy(ownerHierarchy, false, onEnter = { parent ->
      if (parent.isIdeClass) {
        if (parent.isInterface) {
          ideSuperInterfaces.add(parent.name)
        } else {
          ideSuperClasses.add(parent.name)
        }
      }
      true
    })
    return ideSuperClasses to ideSuperInterfaces
  }

  fun presentableElementMightHaveBeenDeclaredInIdeSuperTypes(
      elementType: String,
      ownerHierarchy: ClassHierarchy,
      ideVersion: IdeVersion
  ): String {
    val (ideSuperClasses, ideSuperInterfaces) = findIdeSuperClassesAndInterfaces(ownerHierarchy)
    return if (ideSuperClasses.isEmpty() && ideSuperInterfaces.isEmpty()) {
      ""
    } else buildString {
      append(" The $elementType might have been declared ")
      if (ideSuperClasses.isNotEmpty()) {
        //in one of the $ideVersion
        append("in the super " + "class".pluralize(ideSuperClasses.size) + " belonging to $ideVersion")
        append(" (")
        append(ideSuperClasses.joinToString(transform = toFullJavaClassName))
        append(")")
      }
      if (ideSuperInterfaces.isNotEmpty()) {
        if (ideSuperClasses.isNotEmpty()) {
          append(" or ")
        }
        append("in the super " + "interface".pluralize(ideSuperInterfaces.size) + " belonging to $ideVersion")
        append("(")
        append(ideSuperInterfaces.joinToString(transform = toFullJavaClassName))
        append(")")
      }
    }
  }
}