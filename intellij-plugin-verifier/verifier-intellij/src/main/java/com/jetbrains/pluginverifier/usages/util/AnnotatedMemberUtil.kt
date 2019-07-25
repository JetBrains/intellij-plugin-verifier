package com.jetbrains.pluginverifier.usages.util

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.*

/**
 * Checks if this member (class, method, field) is annotated directly,
 * or if its containing class or package is annotated.
 */
fun ClassFileMember.isMemberEffectivelyAnnotatedWith(annotationName: String, resolver: Resolver): Boolean {
  if (isDirectlyAnnotatedWith(annotationName)) {
    return true
  }

  if (this !is ClassFile) {
    return containingClassFile.isMemberEffectivelyAnnotatedWith(annotationName, resolver)
  }

  if (name.endsWith("package-info")) {
    return false
  }

  val enclosingClassName = enclosingClassName
  if (enclosingClassName != null) {
    val enclosingClass = resolver.resolveClassOrNull(enclosingClassName) ?: return false
    return enclosingClass.isMemberEffectivelyAnnotatedWith(annotationName, resolver)
  }

  val packageName = containingClassFile.packageName
  if (packageName.isNotEmpty()) {
    val packageInfoClass = resolver.resolveClassOrNull("$packageName/package-info")
    if (packageInfoClass != null) {
      return packageInfoClass.isDirectlyAnnotatedWith(annotationName)
    }
  }

  return false
}

private fun ClassFileMember.isDirectlyAnnotatedWith(annotationName: String) =
    runtimeInvisibleAnnotations.findAnnotation(annotationName) != null