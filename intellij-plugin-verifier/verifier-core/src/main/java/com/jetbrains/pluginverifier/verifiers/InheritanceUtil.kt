package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassResolver
import java.util.*

fun ClassResolver.isSubclassOrSelf(childClassName: String, possibleParentName: String): YesNoUnsure {
  if (childClassName == possibleParentName) {
    return YesNoUnsure.YES
  }
  return isSubclassOf(childClassName, possibleParentName)
}

fun ClassResolver.isSubclassOf(childClassName: String, possibleParentName: String): YesNoUnsure {
  val childClass = resolveClassOrNull(childClassName) ?: return YesNoUnsure.UNSURE
  return isSubclassOf(childClass, possibleParentName)
}

fun ClassResolver.isSubclassOf(child: ClassFile, parentName: String): YesNoUnsure {
  if (parentName == "java/lang/Object") {
    return YesNoUnsure.YES
  }

  var unsure = false

  val queue = LinkedList<ClassFile>()

  val directParents = resolveAllDirectParentsOrNull(child)
  unsure = unsure or directParents.any { it == null }

  val resolvedParents = directParents.filterNotNull()
  queue += resolvedParents

  val visited = hashSetOf<String>()
  visited += resolvedParents.map { it.name }

  while (queue.isNotEmpty()) {
    val node = queue.poll()
    if (node.name == parentName) {
      return YesNoUnsure.YES
    }

    val parentClasses = resolveAllDirectParentsOrNull(node)
    unsure = unsure or parentClasses.any { it == null }

    parentClasses.filterNot { it == null || it.name in visited }.forEach {
      visited += it!!.name
      queue.addLast(it)
    }
  }

  return if (unsure) YesNoUnsure.UNSURE else YesNoUnsure.NO
}

private fun ClassResolver.resolveAllDirectParentsOrNull(classFile: ClassFile): List<ClassFile?> {
  val parents = listOfNotNull(classFile.superName) + classFile.interfaces
  return parents.map { resolveClassOrNull(it) }
}