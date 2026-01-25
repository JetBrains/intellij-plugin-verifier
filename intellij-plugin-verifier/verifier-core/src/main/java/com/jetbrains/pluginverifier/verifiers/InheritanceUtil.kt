/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull
import java.util.*

private fun Resolver.resolveAllDirectParents(classFile: ClassFile): List<ClassFile> {
  val parents = listOfNotNull(classFile.superName) + classFile.interfaces
  return parents.mapNotNull { resolveClassOrNull(it) }
}

fun Resolver.isSubclassOrSelf(childClassName: String, possibleParentName: String): Boolean {
  if (childClassName == possibleParentName) {
    return true
  }
  return isSubclassOf(childClassName, possibleParentName)
}

fun Resolver.isSubclassOf(childClassName: String, possibleParentName: String): Boolean {
  if (possibleParentName == "java/lang/Object") {
    return true
  }
  val childClass = resolveClassOrNull(childClassName) ?: return false
  return isSubclassOf(childClass, possibleParentName)
}

fun Resolver.isSubclassOf(child: ClassFile, parentName: String): Boolean {
  if (parentName == "java/lang/Object") {
    return true
  }

  val queue = LinkedList<ClassFile>()
  val visited = hashSetOf<String>()

  queue.add(child)
  visited.add(child.name)

  while (queue.isNotEmpty()) {
    val node = queue.poll()
    if (node.name == parentName) {
      return true
    }

    resolveAllDirectParents(node).forEach {
      if (visited.add(it.name)) {
        queue.addLast(it)
      }
    }
  }

  return false
}