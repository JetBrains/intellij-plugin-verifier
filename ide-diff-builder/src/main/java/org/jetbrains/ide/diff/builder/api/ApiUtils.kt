/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.verifiers.hierarchy.ClassParentsVisitor
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull
import java.util.concurrent.atomic.AtomicBoolean

fun isMethodOverriding(method: Method, resolver: Resolver): Boolean {
  if (method.isConstructor
    || method.isClassInitializer
    || method.isStatic
    || method.isPrivate
    || method.isPackagePrivate
  ) {
    return false
  }
  return hasSuperTypeMatchingPredicate(method.containingClassFile, resolver) { parentClass ->
    parentClass.methods.any {
      it.name == method.name
        && it.descriptor == method.descriptor
        && !it.isStatic
        && !it.isPrivate
        && !it.isPackagePrivate
    }
  }
}

fun hasSuperTypeMatchingPredicate(
  startClass: ClassFile,
  resolver: Resolver,
  predicate: (ClassFile) -> Boolean
): Boolean {
  val parentsVisitor = ClassParentsVisitor(true) { _, parentClassName ->
    resolver.resolveClassOrNull(parentClassName)
  }
  val result = AtomicBoolean()
  parentsVisitor.visitClass(startClass, visitSelf = false, onEnter = { parentClass ->
    val accept = predicate(parentClass)
    if (accept) {
      result.set(true)
    }
    !result.get()
  }, onExit = {})
  return result.get()
}
