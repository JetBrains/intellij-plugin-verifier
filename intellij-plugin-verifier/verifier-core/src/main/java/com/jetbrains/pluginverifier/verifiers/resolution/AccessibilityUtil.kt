/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.isSubclassOf

fun isClassAccessibleToOtherClass(me: ClassFile, other: ClassFile): Boolean =
  me.isPublic
    || me.isPrivate && me.name == other.name
    || me.javaPackageName == other.javaPackageName
    || isKotlinDefaultConstructorMarker(me)

/**
 * In Kotlin classes the default constructor has a special parameter of type `DefaultConstructorMarker`.
 * This class is package-private but is never instantiated because `null` is always passed as its value.
 * We should not report "illegal access" for this class.
 */
private fun isKotlinDefaultConstructorMarker(classFile: ClassFile): Boolean =
  classFile.name == "kotlin/jvm/internal/DefaultConstructorMarker"

fun detectAccessProblem(callee: ClassFileMember, caller: ClassFileMember, context: VerificationContext): AccessType? {
  when {
    callee.isPrivate -> {
      if (callee is Method || callee is Field) {
        val callerClass = if (caller is ClassFile) caller else caller.containingClassFile
        val calleeClass = callee.containingClassFile
        return if (doClassesBelongToTheSameNestHost(callerClass, calleeClass, context)) {
          null
        } else {
          AccessType.PRIVATE
        }
      }
      if (caller.containingClassFile.name != callee.containingClassFile.name) {
        return AccessType.PRIVATE
      }
    }
    callee.isProtected ->
      if (caller.containingClassFile.packageName != callee.containingClassFile.packageName) {
        if (!context.classResolver.isSubclassOf(caller.containingClassFile, callee.containingClassFile.name)) {
          return AccessType.PROTECTED
        }
      }
    callee.isPackagePrivate ->
      if (caller.containingClassFile.packageName != callee.containingClassFile.packageName) {
        return AccessType.PACKAGE_PRIVATE
      }
  }
  return null
}

private fun getClassNestHost(classFile: ClassFile, context: VerificationContext): ClassFile? {
  val nestHostClassName = classFile.nestHostClass ?: return classFile
  return context.classResolver.resolveClassOrNull(nestHostClassName)
}

private fun doClassesBelongToTheSameNestHost(one: ClassFile, two: ClassFile, context: VerificationContext): Boolean {
  if (one.name == two.name) {
    return true
  }
  val oneNestHost = getClassNestHost(one, context)
  val twoNestHost = getClassNestHost(two, context)
  return oneNestHost != null && twoNestHost != null && oneNestHost.name == twoNestHost.name
}