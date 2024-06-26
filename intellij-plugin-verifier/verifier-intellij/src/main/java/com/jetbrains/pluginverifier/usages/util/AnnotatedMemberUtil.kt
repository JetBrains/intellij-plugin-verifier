/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.util

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.verifiers.hasAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassOrNull

sealed class MemberAnnotation {

  abstract val member: ClassFileMember

  abstract val annotationName: String

  class AnnotatedDirectly(
    override val member: ClassFileMember,
    override val annotationName: String
  ) : MemberAnnotation()

  class AnnotatedViaContainingClass(
    val containingClass: ClassFileMember,
    override val member: ClassFileMember,
    override val annotationName: String
  ) : MemberAnnotation()

  class AnnotatedViaPackage(
    val packageName: String,
    override val member: ClassFileMember,
    override val annotationName: String
  ) : MemberAnnotation()
}

fun ClassFileMember.isMemberEffectivelyAnnotatedWith(annotationName: String, resolver: Resolver): Boolean =
  findEffectiveMemberAnnotation(annotationName, resolver) != null

fun ClassFileMember.findEffectiveMemberAnnotation(annotationName: String, resolver: Resolver): MemberAnnotation? {
  if (isDirectlyAnnotatedWith(annotationName)) {
    return MemberAnnotation.AnnotatedDirectly(this, annotationName)
  }

  if (this !is ClassFile) {
    val memberAnnotation = containingClassFile.findEffectiveMemberAnnotation(annotationName, resolver)
    return memberAnnotation?.let { MemberAnnotation.AnnotatedViaContainingClass(containingClassFile, this, annotationName) }
  }

  if (name.endsWith("package-info")) {
    return null
  }

  val enclosingClassName = enclosingClassName
  // If enclosing class name is the same as the current class name the endless loop happens
  // (since the same class will be resolved and findEffectiveMemberAnnotation call leads here)
  if (enclosingClassName != null && enclosingClassName != this.name) {
    val enclosingClass = resolver.resolveClassOrNull(enclosingClassName) ?: return null
    val memberAnnotation = enclosingClass.findEffectiveMemberAnnotation(annotationName, resolver)
    return memberAnnotation?.let { MemberAnnotation.AnnotatedViaContainingClass(enclosingClass, this, annotationName) }
  }

  val packageName = containingClassFile.packageName
  if (packageName.isNotEmpty()) {
    val packageInfoClass = resolver.resolveClassOrNull("$packageName/package-info")
    if (packageInfoClass != null) {
      return if (packageInfoClass.isDirectlyAnnotatedWith(annotationName)) {
        MemberAnnotation.AnnotatedViaPackage(packageName, this, annotationName)
      } else {
        null
      }
    }
  }

  return null
}

private fun ClassFileMember.isDirectlyAnnotatedWith(annotationName: BinaryClassName): Boolean =
  annotations.hasAnnotation(annotationName)